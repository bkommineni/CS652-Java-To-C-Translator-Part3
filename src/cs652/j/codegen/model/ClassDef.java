package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bharu on 3/23/17.
 */
public class ClassDef extends OutputModelObject {
    public String className;
    @ModelElement public List<VarDef> fields = new ArrayList<>();
    @ModelElement public List<MethodDef> methods = new ArrayList<>();
    @ModelElement public List<FuncName> vtable = new ArrayList<>();

    public ClassDef(String className) {
        this.className = className;
    }

    public void addField(VarDef varDef)
    {
        fields.add(varDef);
    }

    public void addMethod(MethodDef methodDef)
    {
        methods.add(methodDef);
    }

    public void addFuncVtable(FuncName funcName)
    {
        vtable.add(funcName);
    }

    public boolean checkDuplicate(String funcName)
    {
        for(int i=0;i<vtable.size();i++)
        {
            if(vtable.get(i).fName.equals(funcName))
            {
                return true;
            }
        }
        return false;
    }
}
