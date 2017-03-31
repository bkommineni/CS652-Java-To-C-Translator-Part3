package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class PrintStat extends Stat {
    public String printStat;
    @ModelElement public List<Expr> args = new ArrayList<>();

    public PrintStat(String printStat) {
        this.printStat = printStat;
    }

    public void addArg(Expr arg)
    {
        args.add(arg);
    }
}
