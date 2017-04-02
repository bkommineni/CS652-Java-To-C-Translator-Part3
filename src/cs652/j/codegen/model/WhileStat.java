package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class WhileStat extends Stat {
    @ModelElement public Expr condition;
    @ModelElement public Stat stat;

    public WhileStat(Expr condition,Stat stat) {
        this.condition = condition;
        this.stat = stat;
    }
}
