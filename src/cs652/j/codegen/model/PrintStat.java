package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class PrintStat extends Stat {
    public String printStmnt;
    @ModelElement public List<VarRef> args;

    public PrintStat(String printStmnt) {
        this.printStmnt = printStmnt;
    }

    public void addArg(VarRef arg)
    {
        args.add(arg);
    }
}
