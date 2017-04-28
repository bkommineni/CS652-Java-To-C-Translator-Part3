package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.Comparator;

/**
 * Created by bharu on 3/23/17.
 */
public class FuncName extends OutputModelObject implements Comparable<FuncName> {
    public String fName;
    public String className;
    public int slot;
    public FuncName(String className,String fName)
    {
        this.className = className;
        this.fName = fName;
    }

    public void setSlot(int slot)
    {
        this.slot = slot;
    }

    @Override
    public int compareTo(FuncName o) {
        return (this.slot - o.slot);
    }
}
