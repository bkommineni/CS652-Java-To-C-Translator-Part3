package cs652.j.codegen;

import com.sun.org.apache.xpath.internal.operations.Variable;
import cs652.j.codegen.model.*;
import cs652.j.parser.JBaseVisitor;
import cs652.j.parser.JParser;
import cs652.j.semantics.*;
import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

		// if superclass is present;inheriting the fields from super class to base class
		if(ctx.superClass != null)
		{
			JClass jClass = (JClass) currentScope.resolve(ctx.superClass.getText());
			for(FieldSymbol field : jClass.getFields())
			{
				VarDef varDef = createVarDef(field);
				classDef.addField(varDef);
			}
		}

		//adding fields and methods of current class
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

		//adding functions to vtable
		Set<MethodSymbol> visibleMethods = currentClass.getMethods();
		for (MemberSymbol s : visibleMethods)
		{
			JMethod jMethod = (JMethod) currentScope.resolve(s.getName());
			FuncName funcName = new FuncName(jMethod.getEnclosingScope().getName(),s.getName());
			funcName.setSlot(jMethod.getSlotNumber());
			classDef.addFuncVtable(funcName);
		}
		Collections.sort(classDef.vtable);
		currentScope = currentScope.getEnclosingScope();
		return classDef;
	}

	public VarDef createVarDef(VariableSymbol variableSymbol)
	{
		VarDef varDef = null;
		if(variableSymbol.getType() instanceof PrimitiveType)
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(variableSymbol.getType());
			varDef = new VarDef(typeSpec,new VarRef(variableSymbol.getName()));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(variableSymbol.getType());
			varDef = new VarDef(typeSpec,new VarRef(variableSymbol.getName()));
		}
		return varDef;
	}

	@Override
	public OutputModelObject visitMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		currentScope = ctx.scope;

		//adding method definition;funcName,Parameters,block
		MethodDef methodDef;
		FuncName funcName = new FuncName(currentClass.getName(),ctx.ID().getText());
		JMethod jMethod = (JMethod) currentClass.resolve(ctx.ID().getText());
		Type methodType = jMethod.getType();

		if(methodType instanceof PrimitiveType)
		{
			methodDef = new MethodDef(currentClass.getName(),funcName,new PrimitiveTypeSpec(methodType));
		}
		else
		{
			methodDef = new MethodDef(currentClass.getName(),funcName,new ObjectTypeSpec(methodType));
		}

		methodDef.addArg(new VarDef(new ObjectTypeSpec((JClass)jMethod.getEnclosingScope()),new ThisRef()));
		if(ctx.formalParameters().formalParameterList() != null)
		{
			for (JParser.FormalParameterContext param : ctx.formalParameters().formalParameterList().formalParameter())
			{
				methodDef.addArg((VarDef) visit(param));
			}
		}
		methodDef.setBlock((Block)visit(ctx.methodBody().block()));

		currentScope = currentScope.getEnclosingScope();
		return methodDef;
	}

	@Override
	public OutputModelObject visitFormalParameter(JParser.FormalParameterContext ctx) {
		JVar param = (JVar) currentScope.resolve(ctx.ID().getText());
		return createVarDef(param);
	}

	@Override
	public OutputModelObject visitBlock(JParser.BlockContext ctx) {
		currentScope = ctx.scope;

		//adding block;local variables and statements
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

		/**
		 * adding main method;which has a standard function prototype
		 * adding function block
		 */
		MainMethod mainMethod = new MainMethod();
		mainMethod.setBlock((Block) visit(ctx.block()));

		currentScope = currentScope.getEnclosingScope();
		return mainMethod;
	}

	@Override
	public OutputModelObject visitFieldDeclaration(JParser.FieldDeclarationContext ctx) {
		VarDef varDef = null;
		JField jField = (JField) currentScope.resolve(ctx.ID().getText());
		varDef = createVarDef(jField);
		return varDef;
	}

	@Override
	public OutputModelObject visitLocalVarStat(JParser.LocalVarStatContext ctx) {
		VarDef varDef = null;
		JVar jVar = (JVar) currentScope.resolve(ctx.localVariableDeclaration().ID().getText());
		varDef = createVarDef(jVar);
		return varDef;
	}

	@Override
	public OutputModelObject visitCtorCall(JParser.CtorCallContext ctx) {
		return new CtorCall(ctx.ID().getText());
	}

	@Override
	public OutputModelObject visitAssignStat(JParser.AssignStatContext ctx) {
		AssignStat assignStat ;
		Expr left = (Expr) visit(ctx.expression(0));
		Expr right = null;

		/**
		 * type-casting right expression if it is not primitive type with the
		 * type of left expression
		 */
		TypeCast typeCast;
		if(ctx.expression(0).type instanceof PrimitiveType)
		{
			right = (Expr) visit(ctx.expression(1));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.expression(0).type);
			typeCast = new TypeCast(typeSpec,(Expr) visit(ctx.expression(1)));
			right = typeCast;
		}

		assignStat = new AssignStat(left,right);
		return assignStat;
	}

	@Override
	public OutputModelObject visitCallStat(JParser.CallStatContext ctx) {
		return new CallStat((MethodCall) visit(ctx.expression()));
	}

	public MethodCall methodCall(JMethod jMethod,Expr receiver,String methodName,String className,
								  List<JParser.ExpressionContext> args)
	{
		MethodCall methodCall = new MethodCall(methodName,className);
		FuncPtrType funcPtrType = null;
		methodCall.setReceiver(receiver);
		Type methodType = jMethod.getType();
		if(methodType instanceof PrimitiveType)
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(methodType);
			funcPtrType = new FuncPtrType(typeSpec);
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(methodType);
			funcPtrType = new FuncPtrType(typeSpec);
		}

		methodCall.setReceiverType(new TypeCast(new ObjectTypeSpec((Type) jMethod.getEnclosingScope()),receiver));
		funcPtrType.addArgType(new ObjectTypeSpec((Type) jMethod.getEnclosingScope()));

		if(args != null)
		{
			for(JParser.ExpressionContext child : args)
			{
				OutputModelObject model = visit(child);
				//to type cast if arguments are not literals to avoid warnings from compiler in c
				if(!(model instanceof LiteralRef))
				{
					TypeCast typeCast1 = new TypeCast(new ObjectTypeSpec(child.type),(Expr)model);
					methodCall.addArg(typeCast1);
					funcPtrType.addArgType(new ObjectTypeSpec(child.type));
				}
				else
				{
					methodCall.addArg((Expr)model);
					funcPtrType.addArgType(new PrimitiveTypeSpec(child.type));
				}
			}
		}
		methodCall.setFptrType(funcPtrType);
		return methodCall;
	}

	@Override
	public OutputModelObject visitQMethodCall(JParser.QMethodCallContext ctx) {

		JClass jClass = (JClass) ctx.expression().type;
		JMethod jMethod = (JMethod) jClass.resolveMember(ctx.ID().getText());

		Expr receiver = (Expr)visit(ctx.expression());
		String methodName = ctx.ID().getText();
		String className = ctx.expression().type.getName();
		List<JParser.ExpressionContext> args = null;
		if(ctx.expressionList() != null)
		{
			args = ctx.expressionList().expression();
		}
		return methodCall(jMethod,receiver,methodName,className,args);
	}

	@Override
	public OutputModelObject visitIdRef(JParser.IdRefContext ctx) {
		Expr expr = null;

		Symbol symbol = currentScope.resolve(ctx.ID().getText());
		if(symbol instanceof JField)
		{
			expr = new FieldRef(ctx.ID().getText(), new ThisRef());
		}
		else
		{
			expr = new VarRef(ctx.ID().getText());
		}
		return expr;
	}

	@Override
	public OutputModelObject visitLiteralRef(JParser.LiteralRefContext ctx) {
		return new LiteralRef(ctx.getText());
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

		JMethod jMethod = (JMethod) currentScope.resolve(ctx.ID().getText());
		Expr receiver = new ThisRef();
		String methodName = ctx.ID().getText();
		String className = currentClass.getName();
		List<JParser.ExpressionContext> args = null;
		if(ctx.expressionList() != null)
		{
			args = ctx.expressionList().expression();
		}
		return methodCall(jMethod,receiver,methodName,className,args);
	}

	@Override
	public OutputModelObject visitWhileStat(JParser.WhileStatContext ctx) {
		WhileStat whileStat = new WhileStat((Expr) visit(ctx.parExpression().expression()),
																(Stat) visit(ctx.statement()));
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
