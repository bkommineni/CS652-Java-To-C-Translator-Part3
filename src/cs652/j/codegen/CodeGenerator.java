package cs652.j.codegen;

import cs652.j.codegen.model.AssignStat;
import cs652.j.codegen.model.Block;
import cs652.j.codegen.model.CFile;
import cs652.j.codegen.model.CallStat;
import cs652.j.codegen.model.ClassDef;
import cs652.j.codegen.model.CtorCall;
import cs652.j.codegen.model.Expr;
import cs652.j.codegen.model.FieldRef;
import cs652.j.codegen.model.FuncName;
import cs652.j.codegen.model.IfElseStat;
import cs652.j.codegen.model.IfStat;
import cs652.j.codegen.model.LiteralRef;
import cs652.j.codegen.model.MainMethod;
import cs652.j.codegen.model.MethodCall;
import cs652.j.codegen.model.MethodDef;
import cs652.j.codegen.model.NullRef;
import cs652.j.codegen.model.ObjectTypeSpec;
import cs652.j.codegen.model.OutputModelObject;
import cs652.j.codegen.model.PrimitiveTypeSpec;
import cs652.j.codegen.model.PrintStat;
import cs652.j.codegen.model.PrintStringStat;
import cs652.j.codegen.model.ReturnStat;
import cs652.j.codegen.model.Stat;
import cs652.j.codegen.model.ThisRef;
import cs652.j.codegen.model.TypeCast;
import cs652.j.codegen.model.TypeSpec;
import cs652.j.codegen.model.VarDef;
import cs652.j.codegen.model.VarRef;
import cs652.j.codegen.model.WhileStat;
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
		return file;
	}

	@Override
	public OutputModelObject visitClassDeclaration(JParser.ClassDeclarationContext ctx) {
		currentScope = ctx.scope;
		currentClass = (JClass) currentScope;
		return super.visitClassDeclaration(ctx);
	}

	@Override
	public OutputModelObject visitClassBody(JParser.ClassBodyContext ctx) {
		ClassDef classDef = new ClassDef(currentClass.getName());
		for(JParser.ClassBodyDeclarationContext child : ctx.classBodyDeclaration())
		{
			OutputModelObject model = visit(child);
			if(model instanceof VarDef)
			{
				classDef.addField((VarDef) model);
			}
			else
			{
				classDef.addMethod((MethodDef) model);
				classDef.addFuncVtable(((MethodDef) model).funcName);
			}
		}
		return classDef;
	}

	@Override
	public OutputModelObject visitMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		FuncName funcName = new FuncName(ctx.ID().getText());
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
		for(JParser.FormalParameterContext child : ctx.formalParameters().formalParameterList().formalParameter())
		{
			OutputModelObject model = visit(child);
			methodDef.addArg((VarDef)model);
		}
		Block body = new Block();
		methodDef.setBlock(body);
		for(JParser.StatementContext child : ctx.methodBody().block().statement())
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
		return methodDef;
	}

	@Override
	public OutputModelObject visitMain(JParser.MainContext ctx) {
		currentScope = ctx.scope;
		FuncName funcName = new FuncName("main");
		PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec("int");
		MainMethod mainMethod = new MainMethod(funcName,typeSpec);
		VarDef varDef1 = new VarDef(new PrimitiveTypeSpec("int"),"argc");
		VarDef varDef2 = new VarDef(new PrimitiveTypeSpec("char*"),"argv[]");
		mainMethod.addArg(varDef1);
		mainMethod.addArg(varDef2);
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
	public OutputModelObject visitLocalVarStat(JParser.LocalVarStatContext ctx) {
		VarDef varDef;
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
				PrimitiveTypeSpec typeSpec = new PrimitiveTypeSpec(ctx.expression(0).type.getName());
				typeCast = new TypeCast(typeSpec,(Expr) visit(ctx.expression(1)));
			}
			else
			{
				ObjectTypeSpec typeSpec = new ObjectTypeSpec(ctx.expression(0).type.getName());
				typeCast = new TypeCast(typeSpec,(Expr) visit(ctx.expression(1)));
			}
			right = typeCast;
		}
		else
		{
			right = (Expr) visit(ctx.expression(1));
		}
		assignStat = new AssignStat(left,right);
		return assignStat;
	}

	@Override
	public OutputModelObject visitIdRef(JParser.IdRefContext ctx) {
		VarRef varRef = new VarRef(ctx.ID().getText());
		return varRef;
	}

	public CFile generate(ParserRuleContext tree) {
		CFile file = (CFile)visit(tree);
		return file;
	}
}
