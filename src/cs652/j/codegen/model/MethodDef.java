package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class MethodDef extends OutputModelObject {
    public String className;
    public int slot;
    @ModelElement public FuncName funcName;
    @ModelElement public TypeSpec returnType;
    @ModelElement public List<VarDef> args = new ArrayList<>();
    @ModelElement public Block body;

    public MethodDef() {
    }

    public MethodDef(String className, FuncName funcName, TypeSpec returnType) {
        this.className = className;
        this.funcName = funcName;
        this.returnType = returnType;
    }

    public MethodDef(FuncName funcName,TypeSpec returnType) {
        this.funcName = funcName;
        this.returnType = returnType;
    }

    public void addArg(VarDef varDef)
    {
        args.add(varDef);
    }

    public void setBlock(Block body)
    {
        this.body = body;
    }

    public void setSlot(int slot)
    {
        this.slot = slot;
    }
}
