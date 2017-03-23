package cs652.j.semantics;

import org.antlr.symtab.ClassSymbol;
import org.antlr.v4.runtime.ParserRuleContext;

public class JClass extends ClassSymbol {
	public JClass(String name, ParserRuleContext tree) {
		super(name);
		setDefNode(tree);
	}
}
