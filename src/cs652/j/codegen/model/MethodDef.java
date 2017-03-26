package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class MethodDef extends OutputModelObject {
    public String className;
    @ModelElement public FuncName funcName;
    @ModelElement public String returnType;
    @ModelElement public List<VarDef> args = new ArrayList<>();
    @ModelElement public List<Stat> body = new ArrayList<>();

    public MethodDef(FuncName funcName,String returnType) {
        this.funcName = funcName;
        this.returnType = returnType;
    }

    public void addArg(VarDef varDef)
    {
        args.add(varDef);
    }

    public void addBody(Stat stat)
    {
        body.add(stat);
    }

    public void setClassName(String className)
    {
        this.className = className;
    }
}
