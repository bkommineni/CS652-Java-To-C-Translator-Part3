package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class IfStat extends Stat {
    @ModelElement public Expr condition;
    @ModelElement public Stat stat;

    public IfStat(Expr condition, Stat stat) {
        this.condition = condition;
        this.stat = stat;
    }
}
