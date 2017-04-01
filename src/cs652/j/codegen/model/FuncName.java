package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class FuncName extends OutputModelObject {
    public String fName;
    public String className;
    public FuncName(String className,String fName)
    {
        this.className = className;
        this.fName = fName;
    }

    public String getFuncName()
    {
        return fName;
    }
}
