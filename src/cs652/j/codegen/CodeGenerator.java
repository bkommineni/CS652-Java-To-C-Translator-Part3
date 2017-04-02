package cs652.j.codegen;

import cs652.j.codegen.model.*;
import cs652.j.parser.JBaseVisitor;
import cs652.j.parser.JParser;
import cs652.j.semantics.JClass;
import cs652.j.semantics.JField;
import cs652.j.semantics.JMethod;
import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import sun.jvm.hotspot.debugger.cdbg.Sym;

import java.util.ArrayList;
import java.util.List;

public class CodeGenerator extends JBaseVisitor<OutputModelObject> {
	public STGroup templates;
	public String fileName;

	public Scope currentScope;
	public JClass currentClass;

	public CodeGenerator(String fileName) {
		this.fileName = fileName;
		templates = new STGroupFile("cs652/j/templates/C.stg");
	}

	@Override
	public OutputModelObject visitFile(JParser.FileContext ctx) {
		currentScope = ctx.scope;
		CFile file = new CFile(fileName);
		for(JParser.ClassDeclarationContext child : ctx.classDeclaration())
		{
			OutputModelObject model = visit(child);
			file.addClass((ClassDef) model);
		}
		file.addMain((MainMethod) visit(ctx.main()));
		currentScope = currentScope.getEnclosingScope();
		return file;
	}

	@Override
	public OutputModelObject visitClassDeclaration(JParser.ClassDeclarationContext ctx) {
		currentScope = ctx.scope;
		currentClass = (JClass) currentScope;
		ClassDef classDef = new ClassDef(currentClass.getName());

		if(ctx.superClass != null)
		{
			JClass jClass = (JClass) currentScope.resolve(ctx.superClass.getText());
			for(FieldSymbol field : jClass.getFields())
			{
				VarDef varDef = null;
				if(field.getType().getName().equals("int") ||
						field.getType().getName().equals("float") )
				{
					PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(field.getType().getName());
					varDef = new VarDef(typeSpec,field.getName());
				}
				else
				{
					ObjectTypeSpec typeSpec = new ObjectTypeSpec(field.getType().getName());
					varDef = new VarDef(typeSpec,field.getName());
				}
				classDef.addField(varDef);
			}
		}

		for(JParser.ClassBodyDeclarationContext child : ctx.classBody().classBodyDeclaration())
		{
			OutputModelObject model = visit(child);
			if(model instanceof VarDef)
			{
				classDef.addField((VarDef) model);
			}
			else
			{
				classDef.addMethod((MethodDef) model);
			}
		}

		if(ctx.superClass != null)
		{
			for(MethodSymbol methodSymbol :currentClass.getSuperClassScope().getMethods())
			{
				JMethod jMethod = (JMethod) currentScope.resolve(methodSymbol.getName());
				FuncName funcName = new FuncName(jMethod.getEnclosingScope().getName(),methodSymbol.getName());
				funcName.setSlot(jMethod.getSlotNumber());
				classDef.addFuncVtable(funcName);
			}

			for(MethodSymbol methodSymbol : currentClass.getMethods())
			{
				JMethod jMethod = (JMethod) currentScope.resolve(methodSymbol.getName());
				if(!classDef.checkDuplicate(methodSymbol.getName()))
				{
					FuncName funcName = new FuncName(jMethod.getEnclosingScope().getName(), methodSymbol.getName());
					funcName.setSlot(jMethod.getSlotNumber());
					classDef.addFuncVtable(funcName);
				}
			}
		}
		else
		{
			for(MethodSymbol methodSymbol :currentClass.getMethods())
			{
				JMethod jMethod = (JMethod) currentScope.resolve(methodSymbol.getName());
				FuncName funcName = new FuncName(jMethod.getEnclosingScope().getName(),methodSymbol.getName());
				funcName.setSlot(jMethod.getSlotNumber());
				classDef.addFuncVtable(funcName);
			}
		}
		currentScope = currentScope.getEnclosingScope();
		return classDef;
	}

	@Override
	public OutputModelObject visitMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		currentScope = ctx.scope;
		FuncName funcName = new FuncName(currentClass.getName(),ctx.ID().getText());

		MethodDef methodDef;
		if(ctx.getChild(0).getText().equals("int") ||
				ctx.getChild(0).getText().equals("float") ||
				ctx.getChild(0).getText().equals("void"))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.getChild(0).getText());
			methodDef = new MethodDef(currentClass.getName(),funcName,typeSpec);
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.getChild(0).getText());
			methodDef = new MethodDef(currentClass.getName(),funcName,typeSpec);
		}
		ObjectTypeSpec typeSpec = new ObjectTypeSpec(currentClass.getName());
		VarDef varDef = new VarDef(typeSpec,"this");
		methodDef.addArg(varDef);
		if(ctx.formalParameters().formalParameterList() != null)
		{
			for (JParser.FormalParameterContext child : ctx.formalParameters().formalParameterList().formalParameter()) {
				OutputModelObject model = visit(child);
				methodDef.addArg((VarDef) model);
			}
		}
		methodDef.setBlock((Block)visit(ctx.methodBody().block()));
		currentScope = currentScope.getEnclosingScope();
		return methodDef;
	}

	@Override
	public OutputModelObject visitBlock(JParser.BlockContext ctx) {
		currentScope = ctx.scope;
		Block body = new Block();
		for(JParser.StatementContext child : ctx.statement())
		{
			OutputModelObject model = visit(child);
			if(model instanceof VarDef)
			{
				body.addLocal((VarDef)model);
			}
			else
			{
				body.addInstr((Stat)model);
			}
		}
		currentScope = currentScope.getEnclosingScope();
		return body;
	}

	@Override
	public OutputModelObject visitMain(JParser.MainContext ctx) {
		currentScope = ctx.scope;
		MainMethod mainMethod = new MainMethod();
		Block body = new Block();
		mainMethod.setBlock(body);
		for(JParser.StatementContext child : ctx.block().statement())
		{
			OutputModelObject model = visit(child);
			if(model instanceof VarDef)
			{
				body.addLocal((VarDef) model);
			}
			else
			{
				body.addInstr((Stat)model);
			}
		}
		currentScope = currentScope.getEnclosingScope();
		return mainMethod;
	}

	@Override
	public OutputModelObject visitFieldDeclaration(JParser.FieldDeclarationContext ctx) {
		VarDef varDef;
		if(ctx.getChild(0).getText().equals("int") ||
				ctx.getChild(0).getText().equals("float"))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,ctx.ID().getText());
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,ctx.ID().getText());
		}
		return varDef;
	}

	@Override
	public OutputModelObject visitFormalParameter(JParser.FormalParameterContext ctx) {
		VarDef varDef = null;
		if(ctx.getChild(0).getText().equals("int") ||
				ctx.getChild(0).getText().equals("float"))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,ctx.ID().getText());
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,ctx.ID().getText());
		}
		return varDef;
	}

	@Override
	public OutputModelObject visitLocalVarStat(JParser.LocalVarStatContext ctx) {
		VarDef varDef = null;
		if(ctx.localVariableDeclaration().jType().getText().equals("int") ||
				ctx.localVariableDeclaration().jType().getText().equals("float"))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.localVariableDeclaration().jType().getText());
			varDef = new VarDef(typeSpec,ctx.localVariableDeclaration().ID().getText());
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.localVariableDeclaration().jType().getText());
			varDef = new VarDef(typeSpec,ctx.localVariableDeclaration().ID().getText());
		}
		return varDef;
	}

	@Override
	public OutputModelObject visitCtorCall(JParser.CtorCallContext ctx) {
		CtorCall ctorCall = new CtorCall(ctx.ID().getText());
		return ctorCall;
	}

	@Override
	public OutputModelObject visitAssignStat(JParser.AssignStatContext ctx) {
		AssignStat assignStat ;
		Expr left = (Expr) visit(ctx.expression(0));
		Expr right = null;
		if(ctx.expression(0).type.getName().equals(ctx.expression(1).type.getName()))
		{
			TypeCast typeCast;
			if(ctx.expression(0).type.getName().equals("int") ||
					ctx.expression(0).type.getName().equals("float"))
			{
				right = (Expr) visit(ctx.expression(1));
			}
			else
			{
				ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.expression(0).type.getName());
				typeCast = new TypeCast(typeSpec,(Expr) visit(ctx.expression(1)));
				right = typeCast;
			}
		}
		else
		{
			right = (Expr) visit(ctx.expression(1));
		}
		assignStat = new AssignStat(left,right);
		return assignStat;
	}

	@Override
	public OutputModelObject visitCallStat(JParser.CallStatContext ctx) {
		CallStat callStat = null;
		MethodCall model = (MethodCall) visit(ctx.expression());
		callStat = new CallStat(model);
		return callStat;
	}

	@Override
	public OutputModelObject visitQMethodCall(JParser.QMethodCallContext ctx) {

		MethodCall methodCall = new MethodCall(ctx.ID().getText(),ctx.expression().type.getName());
		methodCall.setReceiver((Expr)visit(ctx.expression()));
		FuncPtrType funcPtrType = null;
		if(ctx.type.getName().equals("int") ||
				ctx.type.getName().equals("float") ||
				ctx.type.getName().equals("void"))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.type.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.type.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}
		JClass jClass = (JClass) ctx.expression().type;
		JMethod jMethod = (JMethod) jClass.resolveMember(ctx.ID().getText());
		ObjectTypeSpec typeSpec = new ObjectTypeSpec(jMethod.getEnclosingScope().getName());
		funcPtrType.addArgType(typeSpec);
		TypeCast typeCast = new TypeCast(typeSpec,(Expr) visit(ctx.expression()));
		methodCall.addArg(typeCast);

		if(ctx.expressionList() != null)
		{
			for(JParser.ExpressionContext child : ctx.expressionList().expression())
			{
				OutputModelObject model = visit(child);
				if(child.type.getName().equals("int") ||
						child.type.getName().equals("float"))
				{
					PrimitiveTypeSpec primitiveTypeSpec = new PrimitiveTypeSpec(child.type.getName());
					funcPtrType.addArgType(primitiveTypeSpec);
				}
				else
				{
					ObjectTypeSpec objectTypeSpec = new ObjectTypeSpec(child.type.getName());
					funcPtrType.addArgType(objectTypeSpec);
				}
				methodCall.addArg((Expr)model);
			}
		}
		methodCall.setFptrType(funcPtrType);

		return methodCall;
	}

	@Override
	public OutputModelObject visitIdRef(JParser.IdRefContext ctx) {
		VarRef varRef = new VarRef(ctx.ID().getText());
		return varRef;
	}

	@Override
	public OutputModelObject visitLiteralRef(JParser.LiteralRefContext ctx) {
		LiteralRef literalRef = null;
		if(ctx.INT() != null)
		{
			literalRef = new LiteralRef(ctx.INT().getText());
		}
		else
		{
			literalRef = new LiteralRef(ctx.FLOAT().getText());
		}
		return literalRef;
	}

	@Override
	public OutputModelObject visitPrintStringStat(JParser.PrintStringStatContext ctx) {
		PrintStringStat printStringStat = new PrintStringStat(ctx.STRING().getText());
		return printStringStat;
	}

	@Override
	public OutputModelObject visitPrintStat(JParser.PrintStatContext ctx) {
		PrintStat printStat = new PrintStat(ctx.STRING().getText());
		for(JParser.ExpressionContext child : ctx.expressionList().expression())
		{
			OutputModelObject model = visit(child);

			if((model instanceof VarRef) && !currentScope.getName().equals("main"))
			{
				boolean check = false;
				for(Symbol symbol : currentScope.getEnclosingScope().getSymbols())
				{
					if(symbol.getName().equals(((VarRef) model).id))
					{
						check = true;
					}
				}
				if(!check)
				{
					FieldRef fieldRef = new FieldRef(((VarRef) model).id,new ThisRef());
					printStat.addArg(fieldRef);
				}
				else
				{
					printStat.addArg((Expr) model);
				}
			}
			else
			{
				printStat.addArg((Expr) model);
			}
		}
		return printStat;
	}

	@Override
	public OutputModelObject visitReturnStat(JParser.ReturnStatContext ctx) {
		ReturnStat returnStat = null;
		if(ctx.expression() != null)
		{
			OutputModelObject model = visit(ctx.expression());
			if((model instanceof VarRef))
			{
				boolean check = false;
				for(Symbol symbol : currentScope.getEnclosingScope().getSymbols())
				{
					if(symbol.getName().equals(((VarRef) model).id))
					{
						check = true;
					}
				}
				if(!check)
				{
					FieldRef fieldRef = new FieldRef(((VarRef) model).id,new ThisRef());
					returnStat = new ReturnStat(fieldRef);
				}
				else
				{
					returnStat = new ReturnStat((Expr) model);
				}
			}
			else
			{
				returnStat = new ReturnStat((Expr) model);
			}
		}
		else
		{
			returnStat = new ReturnStat();
		}
		return returnStat;
	}

	@Override
	public OutputModelObject visitMethodCall(JParser.MethodCallContext ctx) {
		MethodCall methodCall = new MethodCall(ctx.ID().getText(),currentClass.getName());
		VarRef varRef = new VarRef("this");
		methodCall.setReceiver(varRef);
		FuncPtrType funcPtrType = null;
		if(ctx.type.getName().equals("int") ||
				ctx.type.getName().equals("float") ||
				ctx.type.getName().equals("void"))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.type.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.type.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}
		JMethod jMethod = (JMethod) currentScope.resolve(ctx.ID().getText());
		ObjectTypeSpec typeSpec = new ObjectTypeSpec(jMethod.getEnclosingScope().getName());
		funcPtrType.addArgType(typeSpec);
		TypeCast typeCast = new TypeCast(typeSpec,new ThisRef());
		methodCall.addArg(typeCast);

		if(ctx.expressionList() != null)
		{
			for(JParser.ExpressionContext child : ctx.expressionList().expression())
			{
				OutputModelObject model = visit(child);
				if(child.type.getName().equals("int") ||
						child.type.getName().equals("float"))
				{
					PrimitiveTypeSpec primitiveTypeSpec = new PrimitiveTypeSpec(child.type.getName());
					funcPtrType.addArgType(primitiveTypeSpec);
				}
				else
				{
					ObjectTypeSpec objectTypeSpec = new ObjectTypeSpec(child.type.getName());
					funcPtrType.addArgType(objectTypeSpec);
				}
				methodCall.addArg((Expr)model);
			}
		}
		methodCall.setFptrType(funcPtrType);

		return methodCall;
	}

	@Override
	public OutputModelObject visitWhileStat(JParser.WhileStatContext ctx) {
		WhileStat whileStat = null;
		whileStat = new WhileStat((Expr) visit(ctx.parExpression().expression()),(Stat) visit(ctx.statement()));
		return whileStat;
	}

	@Override
	public OutputModelObject visitThisRef(JParser.ThisRefContext ctx) {
		ThisRef thisRef = new ThisRef();
		return thisRef;
	}

	@Override
	public OutputModelObject visitNullRef(JParser.NullRefContext ctx) {
		NullRef nullRef = new NullRef();
		return nullRef;
	}

	@Override
	public OutputModelObject visitFieldRef(JParser.FieldRefContext ctx) {
		FieldRef fieldRef = new FieldRef(ctx.ID().getText() ,(Expr)visit(ctx.expression()));
		return fieldRef;
	}

	@Override
	public OutputModelObject visitIfStat(JParser.IfStatContext ctx) {
		IfStat ifStat = null;
		if(ctx.getChild(3) != null)
		{
			Expr condition = (Expr) visit(ctx.parExpression().expression());
			Stat ifStmt = (Stat) visit(ctx.statement(0));
			Stat elseStmt = (Stat) visit(ctx.statement(1));
			ifStat = new IfElseStat(condition,ifStmt,elseStmt);
		}
		else
		{
			Expr condition = (Expr) visit(ctx.parExpression().expression());
			Stat ifStmt = (Stat) visit(ctx.statement(0));
			ifStat = new IfStat(condition,ifStmt);
		}
		return ifStat;
	}

	public CFile generate(ParserRuleContext tree) {
		CFile file = (CFile)visit(tree);
		return file;
	}
}
