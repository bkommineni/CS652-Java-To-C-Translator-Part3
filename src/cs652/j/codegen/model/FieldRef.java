package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class FieldRef extends Expr {
    public String id;
    @ModelElement public Expr object;

    public FieldRef(String id, Expr object) {
        this.id = id;
        this.object = object;
    }
}
