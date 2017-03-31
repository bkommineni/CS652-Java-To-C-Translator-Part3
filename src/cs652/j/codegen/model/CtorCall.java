package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class CtorCall extends Expr {
    public String classname;

    public CtorCall(String classname) {
        this.classname = classname;
    }
}
