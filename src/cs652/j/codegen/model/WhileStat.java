package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class WhileStat extends Stat {
    @ModelElement Expr condition;
    @ModelElement Stat stat;

    public WhileStat(Expr condition, Stat stat) {
        this.condition = condition;
        this.stat = stat;
    }
}
