package cs652.j.codegen.model;

/**
 * Created by bharu on 3/23/17.
 */
public class IfElseStat extends IfStat {
    @ModelElement public Stat elseStat;

    public IfElseStat(Expr condition, Stat stat ,Stat elseStat) {
        super(condition, stat);
        this.elseStat = elseStat;
    }
}
