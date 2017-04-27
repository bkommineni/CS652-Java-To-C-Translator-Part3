package cs652.j.codegen;

import cs652.j.codegen.model.*;
import cs652.j.parser.JBaseVisitor;
import cs652.j.parser.JParser;
import cs652.j.semantics.*;
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

		// if superclass is present;inheriting the fields from super class to base class
		if(ctx.superClass != null)
		{
			JClass jClass = (JClass) currentScope.resolve(ctx.superClass.getText());
			for(FieldSymbol field : jClass.getFields())
			{
				VarDef varDef = null;
//  you should not be testing whether their names are "int" using string compare. you computed types and compute type phase right?
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
		//if superclass present;
		if(ctx.superClass != null)
		{
// whoa. multiple loops? why are you looking at the superclass? shouldn't the symbol table answer questions for you properly?
			/**
			 * getting all superclass methods and resolving from current scope; so that overridden methods
			 * when resolved return the enclosing scope in which they are implemented
			 */
// resolve does this automatically
			for(MethodSymbol methodSymbol :currentClass.getSuperClassScope().getMethods())
			{
				JMethod jMethod = (JMethod) currentScope.resolve(methodSymbol.getName());
				FuncName funcName = new FuncName(jMethod.getEnclosingScope().getName(),methodSymbol.getName());
				funcName.setSlot(jMethod.getSlotNumber());
				classDef.addFuncVtable(funcName);
			}

			/**
			 * To make sure not to miss the methods which are added to sub-classes explicity,checking current
			 * class methods and making sure not to add the duplicates
			 */
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
			/**
			 * if superclass is not present;adding the current class methods to vtable
			 */
			for(MethodSymbol methodSymbol :currentClass.getMethods())
			{
				JMethod jMethod = (JMethod) currentScope.resolve(methodSymbol.getName());
				FuncName funcName = new FuncName(jMethod.getEnclosingScope().getName(),methodSymbol.getName());
				funcName.setSlot(jMethod.getSlotNumber());
				classDef.addFuncVtable(funcName);
			}
		}
// all of that above code should be simply
//		Set<MethodSymbol> visibleMethods = getMethods();
//		for (MemberSymbol s : visibleMethods) {...}

		currentScope = currentScope.getEnclosingScope();
		return classDef;
	}

	@Override
	public OutputModelObject visitMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		currentScope = ctx.scope;

		//adding method definition;funcName,Parameters,block
		MethodDef methodDef;
		FuncName funcName = new FuncName(currentClass.getName(),ctx.ID().getText());
		JMethod jMethod = (JMethod) currentClass.resolve(ctx.ID().getText());
		Type methodType = jMethod.getType();
// you should not be testing types as strings
		if(methodType.getName().equals(JINT_TYPE.getName()) ||
				methodType.getName().equals(JFLOAT_TYPE.getName()) ||
				methodType.getName().equals(JVOID_TYPE.getName()))
		{
// you know the type. why are you creating it again? we have a compute type phase
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(methodType.getName());
			methodDef = new MethodDef(currentClass.getName(),funcName,typeSpec);
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(methodType.getName());
			methodDef = new MethodDef(currentClass.getName(),funcName,typeSpec);
		}
// shouldn't you be calling visitFormalParameter() somewhere?

		for(Symbol symbol : jMethod.getSymbols())
		{
			VariableSymbol var = (VariableSymbol) symbol;
			Type varType = var.getType();
			if(varType.getName().equals(JINT_TYPE.getName()) ||
					varType.getName().equals(JFLOAT_TYPE.getName()) ||
					varType.getName().equals(JVOID_TYPE.getName()))
			{
				PrimitiveTypeSpec typeSpec1 = new PrimitiveTypeSpec(varType.getName());
				methodDef.addArg(new VarDef(typeSpec1,new VarRef(var.getName())));
			}
			else
			{
				ObjectTypeSpec typeSpec1 = new ObjectTypeSpec(varType.getName());
				methodDef.addArg(new VarDef(typeSpec1,new VarRef(var.getName())));
			}
		}
		methodDef.setBlock((Block)visit(ctx.methodBody().block()));

		currentScope = currentScope.getEnclosingScope();
		return methodDef;
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
		Type fieldType = jField.getType();
// remove all of these examples of code that are testing strings. furthermore, you don't need to test the type. Just use it
		if(fieldType.getName().equals(JINT_TYPE.getName()) ||
				fieldType.getName().equals(JFLOAT_TYPE.getName()))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(fieldType.getName());
			varDef = new VarDef(typeSpec,new VarRef(ctx.ID().getText()));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(fieldType.getName());
			varDef = new VarDef(typeSpec,new VarRef(ctx.ID().getText()));
		}
		return varDef;
	}

// wow.  Mine is two lines long. Get rid of the type testing.
	@Override
	public OutputModelObject visitLocalVarStat(JParser.LocalVarStatContext ctx) {
		VarDef varDef = null;
		JVar jVar = (JVar) currentScope.resolve(ctx.localVariableDeclaration().ID().getText());
		Type jVarType = jVar.getType();
		if(jVarType.getName().equals(JINT_TYPE.getName()) ||
				jVarType.getName().equals(JFLOAT_TYPE.getName()))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(jVarType.getName());
			varDef = new VarDef(typeSpec,new VarRef(ctx.localVariableDeclaration().ID().getText()));
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(jVarType.getName());
			varDef = new VarDef(typeSpec,new VarRef(ctx.localVariableDeclaration().ID().getText()));
		}
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
		return new CallStat((MethodCall) visit(ctx.expression()));
	}

	@Override
	public OutputModelObject visitQMethodCall(JParser.QMethodCallContext ctx) {

		MethodCall methodCall = new MethodCall(ctx.ID().getText(),ctx.expression().type.getName());
		methodCall.setReceiver((Expr)visit(ctx.expression()));
		FuncPtrType funcPtrType = null;

		/**
		 * As converting from java to c;first argument always needs to be
		 * of its own type;from receiver`s vtable,calculate receiverType to
		 * implement polymorphism using resolve from the type of receiver;
		 */
		JClass jClass = (JClass) ctx.expression().type;
		JMethod jMethod = (JMethod) jClass.resolveMember(ctx.ID().getText());
		Type methodType = jMethod.getType();
		if(methodType.getName().equals(JINT_TYPE.getName()) ||
				methodType.getName().equals(JFLOAT_TYPE.getName()) ||
				methodType.getName().equals(JVOID_TYPE.getName()))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(methodType.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(methodType.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}
// why are you walking the argument list multiple times? here and then in the next loop?
		for(Symbol symbol : jMethod.getSymbols())
		{
			VariableSymbol var = (VariableSymbol) symbol;
			Type varType = var.getType();
			if(varType.getName().equals(JINT_TYPE.getName()) ||
					varType.getName().equals(JFLOAT_TYPE.getName()) ||
					varType.getName().equals(JVOID_TYPE.getName()))
			{
				PrimitiveTypeSpec typeSpec1 = new PrimitiveTypeSpec(varType.getName());
				funcPtrType.addArgType(typeSpec1);
			}
			else
			{
				ObjectTypeSpec typeSpec1 = new ObjectTypeSpec(varType.getName());
				TypeCast typeCast = new TypeCast(typeSpec1,(Expr) visit(ctx.expression()));
				methodCall.setReceiverType(typeCast);
				funcPtrType.addArgType(typeSpec1);
			}
		}

		//if arguments to methodcall are present
		if(ctx.expressionList() != null)
		{
			for(JParser.ExpressionContext child : ctx.expressionList().expression())
			{
				OutputModelObject model = visit(child);
				//to type cast if arguments are not literals to avoid warnings from compiler in c
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

		/**
		 * if VarRefs does not belong to main method scope;they need to be accessed
		 * with implicit this parameter of its own class type(FieldRefs), if not declared in
		 * local scope or method scope
		 */
		if(!currentScope.getName().equals("main")) { // you should not be testing whether you are in the main program
// you only care whether it is a field or a variable
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
// use ctx.getText() and there is no need to separate the cases
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
		JMethod jMethod = (JMethod) currentScope.resolve(ctx.ID().getText());
		Type methodType = jMethod.getType();
		if(methodType.getName().equals(JINT_TYPE.getName()) ||
				methodType.getName().equals(JFLOAT_TYPE.getName()) ||
				methodType.getName().equals(JVOID_TYPE.getName()))
		{
			PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(methodType.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}
		else
		{
			ObjectTypeSpec typeSpec = new ObjectTypeSpec(methodType.getName());
			funcPtrType = new FuncPtrType(typeSpec);
		}

		/**
		 * As converting from java to c;first argument always needs to be
		 * of its own type;from receiver`s vtable,calculate receiverType to
		 * implement polymorphism using resolve from the type of receiver(as
		 * this is a method call;receiver is object of its own class(this));
		 */

// this is almost exactly like your qualified method call so re-factor to share code

		for(Symbol symbol : jMethod.getSymbols())
		{
			VariableSymbol var = (VariableSymbol) symbol;
			Type varType = var.getType();
			if(varType.getName().equals(JINT_TYPE.getName()) ||
					varType.getName().equals(JFLOAT_TYPE.getName()) ||
					varType.getName().equals(JVOID_TYPE.getName()))
			{
				PrimitiveTypeSpec typeSpec1 = new PrimitiveTypeSpec(varType.getName());
				funcPtrType.addArgType(typeSpec1);
			}
			else
			{
				ObjectTypeSpec typeSpec1 = new ObjectTypeSpec(varType.getName());
				TypeCast typeCast = null;
				if(var.getName().equals("this"))
				{
					typeCast = new TypeCast(typeSpec1,new ThisRef());
				}
				else
				{
					typeCast = new TypeCast(typeSpec1,new VarRef(var.getName()));
				}
				methodCall.setReceiverType(typeCast);
				funcPtrType.addArgType(typeSpec1);
			}
		}

		//if arguments to Methodcall are present
		if(ctx.expressionList() != null)
		{
			for(JParser.ExpressionContext child : ctx.expressionList().expression())
			{
				OutputModelObject model = visit(child);
				//type-casting arguments if they are not literals
				if(!(model instanceof LiteralRef))
				{
					TypeCast typeCast = new TypeCast(new ObjectTypeSpec(child.type.getName()),(Expr)model);
					methodCall.addArg(typeCast);
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

		//checking if else statement is present
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
