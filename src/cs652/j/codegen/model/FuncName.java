package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class FuncName extends OutputModelObject {
    public String funcName;
    public String className;
    public FuncName(String className,String funcName)
    {
        this.className = className;
        this.funcName = funcName;
    }

    public String getFuncName()
    {
        return funcName;
    }
}
