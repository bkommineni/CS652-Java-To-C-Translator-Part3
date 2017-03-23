package cs652.j.semantics;

import cs652.j.parser.JBaseListener;
import cs652.j.parser.JParser;
import org.antlr.symtab.GlobalScope;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Type;
import org.antlr.symtab.TypedSymbol;


/**
 * Class which overrides methods of BaseListener to compute types of expressions using scopes
 * and symbols defined in DefineScopesAndSymbols class
 * @author bhargavi
 */
public class ComputeTypes extends JBaseListener {

	protected StringBuilder buf = new StringBuilder();

	public static final Type JINT_TYPE = new JPrimitiveType("int");
	public static final Type JFLOAT_TYPE = new JPrimitiveType("float");
	public static final Type JSTRING_TYPE = new JPrimitiveType("string");
	public static final Type JVOID_TYPE = new JPrimitiveType("void");
	public Scope currentScope;

	public ComputeTypes(GlobalScope globals) {
		this.currentScope = globals;
	}

	@Override
	public void enterFile(JParser.FileContext ctx) {
		currentScope = ctx.scope;
	}

	@Override
	public void exitFile(JParser.FileContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterMain(JParser.MainContext ctx) {
		currentScope = ctx.scope;
	}

	@Override
	public void exitMain(JParser.MainContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterClassDeclaration(JParser.ClassDeclarationContext ctx) {
		currentScope = ctx.scope;
	}

	@Override
	public void exitClassDeclaration(JParser.ClassDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		currentScope = ctx.scope;
	}

	@Override
	public void exitMethodDeclaration(JParser.MethodDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void enterBlock(JParser.BlockContext ctx) {
		currentScope = ctx.scope;
	}

	@Override
	public void exitBlock(JParser.BlockContext ctx) {
		currentScope = currentScope.getEnclosingScope();
	}

	@Override
	public void exitFieldRef(JParser.FieldRefContext ctx) {
		JParser.ExpressionContext exp = (JParser.ExpressionContext)ctx.getChild(0);
		JClass symbol = (JClass) exp.type;
		TypedSymbol type = (TypedSymbol) symbol.resolveMember(ctx.ID().getText());
		ctx.type = type.getType();
		buf.append(ctx.getText() + " is " + type.getType().getName() + "\n");
	}

	@Override
	public void exitQMethodCall(JParser.QMethodCallContext ctx) {
		JParser.ExpressionContext exp = (JParser.ExpressionContext)ctx.getChild(0);
		JClass symbol = (JClass) exp.type;
		TypedSymbol type = (TypedSymbol) symbol.resolveMember(ctx.ID().getText());
		ctx.type = type.getType();
		buf.append(ctx.getText() + " is " + type.getType().getName() + "\n");
	}

	@Override
	public void enterMethodCall(JParser.MethodCallContext ctx) {
		JMethod type = (JMethod) currentScope.resolve(ctx.ID().getText());
		ctx.type = type.getType();
		buf.append(ctx.getText() + " is " + type.getType().getName() + "\n");
	}

	@Override
	public void enterCtorCall(JParser.CtorCallContext ctx) {
		JClass type = (JClass) currentScope.resolve(ctx.ID().getText());
		ctx.type = type;
		buf.append(ctx.getText() + " is "+ type.getName() + "\n");
	}

	@Override
	public void enterThisRef(JParser.ThisRefContext ctx) {
		TypedSymbol type = (TypedSymbol)currentScope.resolve(ctx.getText());
		ctx.type = type.getType();
		buf.append(ctx.getText() + " is " + type.getType().getName() + "\n");
	}

	@Override
	public void enterNullRef(JParser.NullRefContext ctx) {
		ctx.type = JVOID_TYPE;
	}

	@Override
	public void enterLiteralRef(JParser.LiteralRefContext ctx) {
		if(ctx.INT() != null)
		{
			ctx.type = JINT_TYPE;
			buf.append(ctx.getText() + " is " + JINT_TYPE.getName() + "\n");
		}
		else if(ctx.FLOAT() != null)
		{
			ctx.type = JFLOAT_TYPE;
			buf.append(ctx.getText() + " is " + JFLOAT_TYPE.getName() + "\n");
		}
	}

	@Override
	public void enterIdRef(JParser.IdRefContext ctx) {
		TypedSymbol type = (TypedSymbol)currentScope.resolve(ctx.ID().getText());
		ctx.type = type.getType();
		buf.append(ctx.getText() + " is " + type.getType().getName() + "\n");
	}

	public String getRefOutput() {
		return buf.toString();
	}
}

