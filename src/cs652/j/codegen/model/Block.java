package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class Block extends Stat {
    @ModelElement public List<VarDef> locals = new ArrayList<>();
    @ModelElement public List<Stat> instrs = new ArrayList<>();

    public void addLocal(VarDef varDef)
    {
        locals.add(varDef);
    }

    public void addInstr(Stat stat)
    {
        instrs.add(stat);
    }
}
