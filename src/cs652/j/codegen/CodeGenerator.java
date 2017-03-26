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
		for(ParseTree child : ctx.children)
		{
			OutputModelObject model = visit(child);
			if(model instanceof ClassDef)
			{
				file.addClass((ClassDef) model);
			}
			else
				file.addMain((MainMethod) model);
		}
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
		MethodDef methodDef = new MethodDef(funcName,ctx.getChild(0).getText());
		methodDef.setClassName(currentClass.getName());
		for(JParser.FormalParameterContext child : ctx.formalParameters().formalParameterList().formalParameter())
		{
			OutputModelObject model = visit(child);
			methodDef.addArg((VarDef)model);
		}
		for(JParser.StatementContext child : ctx.methodBody().block().statement())
		{
			OutputModelObject model = visit(child);
			methodDef.addBody((Stat)model);
		}
		return methodDef;
	}

	@Override
	public OutputModelObject visitMain(JParser.MainContext ctx) {
		currentScope = ctx.scope;
		FuncName funcName = new FuncName("main");
		MainMethod mainMethod = new MainMethod(funcName,"int");
		VarDef varDef1 = new VarDef("int","argc");
		VarDef varDef2 = new VarDef("char*","argv[]");
		mainMethod.addArg(varDef1);
		mainMethod.addArg(varDef2);
		for(JParser.StatementContext child : ctx.block().statement())
		{
			OutputModelObject model = visit(child);
			mainMethod.addBody((Stat)model);
		}
		return mainMethod;
	}

	@Override
	public OutputModelObject visitFieldDeclaration(JParser.FieldDeclarationContext ctx) {
		VarDef varDef = new VarDef(ctx.jType().getText(),ctx.ID().getText());
		return varDef;
	}

	@Override
	public OutputModelObject visitFormalParameter(JParser.FormalParameterContext ctx) {
		VarDef varDef = new VarDef(ctx.jType().getText(),ctx.ID().getText());
		return varDef;
	}

	public CFile generate(ParserRuleContext tree) {
		CFile file = (CFile)visit(tree);
		return file;
	}
}
