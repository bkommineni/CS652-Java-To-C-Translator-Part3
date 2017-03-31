package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class LiteralRef extends Expr {
    public String id;

    public LiteralRef(String id) {
        this.id = id;
    }
}
