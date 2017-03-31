package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class VarDef extends OutputModelObject {
    @ModelElement public TypeSpec type;
    public String id;

    public VarDef(TypeSpec type, String id) {
        this.type = type;
        this.id = id;
    }
}
