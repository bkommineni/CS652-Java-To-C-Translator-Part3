package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class ReturnStat extends Stat {
    @ModelElement public Expr expr;

    public ReturnStat(Expr expr) {
        this.expr = expr;
    }

    public ReturnStat() {
    }
}
