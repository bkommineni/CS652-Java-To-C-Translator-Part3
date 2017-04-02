package cs652.j.codegen;

import cs652.j.codegen.model.*;
import cs652.j.parser.JBaseVisitor;
import cs652.j.parser.JParser;
import cs652.j.semantics.JClass;
import cs652.j.semantics.JMethod;
import cs652.j.semantics.JPrimitiveType;
import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class CodeGenerator extends JBaseVisitor<OutputModelObject> {

	public static final Type JINT_TYPE = new JPrimitiveType("int");
	public static final Type JFLOAT_TYPE = new JPrimitiveType("float");
	public static final Type JVOID_TYPE = new JPrimitiveType("void");

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
				if(field.getType().getName().equals(JINT_TYPE.getName()) ||
						field.getType().getName().equals(JFLOAT_TYPE.getName()) )
				{
					PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(field.getType().getName());
					varDef = new VarDef(typeSpec,new VarRef(field.getName()));
				}
				else
				{
					ObjectTypeSpec typeSpec = new ObjectTypeSpec(field.getType().getName());
					varDef = new VarDef(typeSpec,new VarRef(field.getName()));
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
		if(ctx.getChild(0).getText().equals(JINT_TYPE.getName()) ||
				ctx.getChild(0).getText().equals(JFLOAT_TYPE.getName()) ||
				ctx.getChild(0).getText().equals(JVOID_TYPE.getName()))
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
		VarDef varDef = new VarDef(typeSpec,new ThisRef());
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
		if(ctx.getChild(0).getText().equals(JINT_TYPE.getName()) ||
				ctx.getChild(0).getText().equals(JFLOAT_TYPE.getName()))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,new VarRef(ctx.ID().getText()));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,new VarRef(ctx.ID().getText()));
		}
		return varDef;
	}

	@Override
	public OutputModelObject visitFormalParameter(JParser.FormalParameterContext ctx) {
		VarDef varDef = null;
		if(ctx.getChild(0).getText().equals(JINT_TYPE.getName()) ||
				ctx.getChild(0).getText().equals(JFLOAT_TYPE.getName()))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,new VarRef(ctx.ID().getText()));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.getChild(0).getText());
			varDef = new VarDef(typeSpec,new VarRef(ctx.ID().getText()));
		}
		return varDef;
	}

	@Override
	public OutputModelObject visitLocalVarStat(JParser.LocalVarStatContext ctx) {
		VarDef varDef = null;
		if(ctx.localVariableDeclaration().jType().getText().equals(JINT_TYPE.getName()) ||
				ctx.localVariableDeclaration().jType().getText().equals(JFLOAT_TYPE.getName()))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.localVariableDeclaration().jType().getText());
			varDef = new VarDef(typeSpec,new VarRef(ctx.localVariableDeclaration().ID().getText()));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.localVariableDeclaration().jType().getText());
			varDef = new VarDef(typeSpec,new VarRef(ctx.localVariableDeclaration().ID().getText()));
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

		TypeCast typeCast;
		if(ctx.expression(0).type.getName().equals(JINT_TYPE.getName()) ||
				ctx.expression(0).type.getName().equals(JFLOAT_TYPE.getName()))
		{
			right = (Expr) visit(ctx.expression(1));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.expression(0).type.getName());
			typeCast = new TypeCast(typeSpec,(Expr) visit(ctx.expression(1)));
			right = typeCast;
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
		if(ctx.type.getName().equals(JINT_TYPE.getName()) ||
				ctx.type.getName().equals(JFLOAT_TYPE.getName()) ||
				ctx.type.getName().equals(JVOID_TYPE.getName()))
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
		methodCall.setReceiverType(typeCast);

		if(ctx.expressionList() != null)
		{
			for(JParser.ExpressionContext child : ctx.expressionList().expression())
			{
				OutputModelObject model = visit(child);
				if(child.type.getName().equals(JINT_TYPE.getName()) ||
						child.type.getName().equals(JFLOAT_TYPE.getName()))
				{
					PrimitiveTypeSpec primitiveTypeSpec = new PrimitiveTypeSpec(child.type.getName());
					funcPtrType.addArgType(primitiveTypeSpec);
				}
				else
				{
					ObjectTypeSpec objectTypeSpec = new ObjectTypeSpec(child.type.getName());
					funcPtrType.addArgType(objectTypeSpec);
				}
				if(!(model instanceof LiteralRef))
				{
					TypeCast typeCast1 = new TypeCast(new ObjectTypeSpec(child.type.getName()),(Expr)model);
					methodCall.addArg(typeCast1);
				}
				else
				{
					methodCall.addArg((Expr)model);
				}
			}
		}
		methodCall.setFptrType(funcPtrType);

		return methodCall;
	}

	@Override
	public OutputModelObject visitIdRef(JParser.IdRefContext ctx) {
		Expr expr = null;
		if(!currentScope.getName().equals("main")) {
			Symbol symbol = currentScope.resolve(ctx.ID().getText());
			if (symbol.getScope().getName().equals(currentScope.getName()) ||
					symbol.getScope().getName().equals(currentScope.getEnclosingScope().getName())) {
				expr = new VarRef(ctx.ID().getText());
			} else {
				expr = new FieldRef(ctx.ID().getText(), new ThisRef());
			}
		}
		else
		{
			expr = new VarRef(ctx.ID().getText());
		}
		return expr;
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
		return new PrintStringStat(ctx.STRING().getText());
	}

	@Override
	public OutputModelObject visitPrintStat(JParser.PrintStatContext ctx) {
		PrintStat printStat = new PrintStat(ctx.STRING().getText());
		for(JParser.ExpressionContext child : ctx.expressionList().expression())
		{
			printStat.addArg((Expr)visit(child));
		}
		return printStat;
	}

	@Override
	public OutputModelObject visitReturnStat(JParser.ReturnStatContext ctx) {
		ReturnStat returnStat = null;
		if(ctx.expression() != null)
		{
			returnStat = new ReturnStat((Expr)visit(ctx.expression()));
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
		methodCall.setReceiver(new ThisRef());
		FuncPtrType funcPtrType = null;
		if(ctx.type.getName().equals(JINT_TYPE.getName()) ||
				ctx.type.getName().equals(JFLOAT_TYPE.getName()) ||
				ctx.type.getName().equals(JVOID_TYPE.getName()))
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
		methodCall.setReceiverType(typeCast);

		if(ctx.expressionList() != null)
		{
			for(JParser.ExpressionContext child : ctx.expressionList().expression())
			{
				OutputModelObject model = visit(child);
				if(child.type.getName().equals(JINT_TYPE.getName()) ||
						child.type.getName().equals(JFLOAT_TYPE.getName()))
				{
					PrimitiveTypeSpec primitiveTypeSpec = new PrimitiveTypeSpec(child.type.getName());
					funcPtrType.addArgType(primitiveTypeSpec);
				}
				else
				{
					ObjectTypeSpec objectTypeSpec = new ObjectTypeSpec(child.type.getName());
					funcPtrType.addArgType(objectTypeSpec);
				}
				if(!(model instanceof LiteralRef))
				{
					TypeCast typeCast1 = new TypeCast(new ObjectTypeSpec(child.type.getName()),(Expr)model);
					methodCall.addArg(typeCast1);
				}
				else
				{
					methodCall.addArg((Expr)model);
				}
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
	public OutputModelObject visitBlockStat(JParser.BlockStatContext ctx) {
		return visit(ctx.block());
	}

	@Override
	public OutputModelObject visitThisRef(JParser.ThisRefContext ctx) {
		return new ThisRef();
	}

	@Override
	public OutputModelObject visitNullRef(JParser.NullRefContext ctx) {
		return new NullRef();
	}

	@Override
	public OutputModelObject visitFieldRef(JParser.FieldRefContext ctx) {
		return new FieldRef(ctx.ID().getText() ,(Expr)visit(ctx.expression()));
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
