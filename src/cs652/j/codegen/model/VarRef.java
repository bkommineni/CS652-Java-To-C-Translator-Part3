package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class VarRef extends Expr {
    public String id;

    public VarRef(String id) {
        this.id = id;
    }
}
