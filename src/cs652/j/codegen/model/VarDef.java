package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class VarDef extends OutputModelObject {
    public String type;
    public String id;

    public VarDef(String type, String id) {
        this.type = type;
        this.id = id;
    }
}
