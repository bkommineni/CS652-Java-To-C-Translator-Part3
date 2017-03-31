package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class PrintStringStat extends Stat {
    public String printStat;

    public PrintStringStat(String printStat) {
        this.printStat = printStat;
    }
}
