package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class TypeCast extends Expr {
    @ModelElement public TypeSpec type;
    @ModelElement public Expr expr;

    public TypeCast(TypeSpec type, Expr expr) {
        this.type = type;
        this.expr = expr;
    }
}
