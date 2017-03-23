package cs652.j.semantics;

import cs652.j.parser.JBaseListener;
import cs652.j.parser.JParser;
import org.antlr.symtab.GlobalScope;
import org.antlr.symtab.LocalScope;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Type;

/**
 * Class which overrides methods of BaseListener and defines all scopes and symbols based on grammar
 * @author bhargavi
 */
public class DefineScopesAndSymbols extends JBaseListener {

	public Scope currentScope;


	public DefineScopesAndSymbols(GlobalScope globals) {
		currentScope = globals;
	}

	@Override
	public void enterFile(JParser.FileContext ctx) {
		currentScope.define((JPrimitiveType) ComputeTypes.JINT_TYPE);
		currentScope.define((JPrimitiveType) ComputeTypes.JFLOAT_TYPE);
		currentScope.define((JPrimitiveType) ComputeTypes.JSTRING_TYPE);
		currentScope.define((JPrimitiveType) ComputeTypes.JVOID_TYPE);
		ctx.scope = (GlobalScope) currentScope;
	}

	@Override
	public void exitFile(JParser.FileContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterClassDeclaration(JParser.ClassDeclarationContext ctx) {
		JClass jClass = new JClass(ctx.name.getText(),ctx);
		jClass.setEnclosingScope(currentScope);
		jClass.setDefNode(ctx);
		if(ctx.superClass != null)
		{
			jClass.setSuperClass(ctx.superClass.getText());
		}
		currentScope.define(jClass);
		currentScope = jClass;
		ctx.scope = (JClass) currentScope;
	}

	@Override
	public void exitClassDeclaration(JParser.ClassDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterMain(JParser.MainContext ctx) {
		JMethod jMethod = new JMethod("main",ctx);
		jMethod.setEnclosingScope(currentScope);
		jMethod.setDefNode(ctx);
		jMethod.setType(ComputeTypes.JVOID_TYPE);
		currentScope.define(jMethod);
		currentScope = jMethod;
		ctx.scope = (JMethod) currentScope;
	}

	@Override
	public void exitMain(JParser.MainContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterBlock(JParser.BlockContext ctx) {
		LocalScope localScope = new LocalScope(currentScope);
		currentScope.nest(localScope);
		currentScope = localScope;
		ctx.scope = (LocalScope) currentScope;
	}

	@Override
	public void exitBlock(JParser.BlockContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		JMethod jMethod = new JMethod(ctx.ID().getText(),ctx);
		jMethod.setEnclosingScope(currentScope);
		jMethod.setDefNode(ctx);
		Type type = (Type) currentScope.resolve(ctx.getChild(0).getText());
		jMethod.setType(type);
		currentScope.define(jMethod);
		JArg implicitThis = new JArg("this");
		implicitThis.setType((Type) currentScope);
		currentScope = jMethod;
		implicitThis.setScope(currentScope);
		currentScope.define(implicitThis);
		ctx.scope = (JMethod) currentScope;
	}

	@Override
	public void exitMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterFieldDeclaration(JParser.FieldDeclarationContext ctx) {
		JField jField = new JField(ctx.ID().getText());
		jField.setScope(currentScope);
		jField.setDefNode(ctx);
		Type type = (Type) currentScope.resolve(ctx.jType().getText());
		jField.setType(type);
		currentScope.define(jField);
	}

	@Override
	public void enterLocalVarStat(JParser.LocalVarStatContext ctx) {
		JVar jVar = new JVar(ctx.localVariableDeclaration().ID().getText());
		jVar.setScope(currentScope);
		jVar.setDefNode(ctx);
		Type type = (Type) currentScope.resolve(ctx.localVariableDeclaration().jType().getText());
		jVar.setType(type);
		currentScope.define(jVar);
	}

	@Override
	public void enterFormalParameter(JParser.FormalParameterContext ctx) {
		JVar param = new JVar(ctx.ID().getText());
		Type type = (Type) currentScope.resolve(ctx.jType().getText());
		param.setType(type);
		currentScope.define(param);
	}
}
