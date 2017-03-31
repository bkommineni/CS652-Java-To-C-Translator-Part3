package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class FuncPtrType extends OutputModelObject {
    @ModelElement public TypeSpec returnType;
    @ModelElement public List<TypeSpec> argTypes = new ArrayList<>();

    public FuncPtrType(TypeSpec returnType) {
        this.returnType = returnType;
    }

    public void addArgType(TypeSpec typeSpec)
    {
        argTypes.add(typeSpec);
    }
}
