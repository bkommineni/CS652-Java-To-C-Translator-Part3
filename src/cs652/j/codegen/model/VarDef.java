package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class VarDef extends OutputModelObject {
    @ModelElement public TypeSpec type;
    @ModelElement public Expr id;

    public VarDef(TypeSpec type, Expr id) {
        this.type = type;
        this.id = id;
    }
}
