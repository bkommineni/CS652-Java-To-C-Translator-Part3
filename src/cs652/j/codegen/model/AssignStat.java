package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class AssignStat extends Stat {
    @ModelElement public Expr left;
    @ModelElement public Expr right;

    public AssignStat(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }
}
