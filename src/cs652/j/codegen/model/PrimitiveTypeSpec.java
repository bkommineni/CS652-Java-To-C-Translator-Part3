package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class PrimitiveTypeSpec extends TypeSpec {
    public String name;

    public PrimitiveTypeSpec(String name) {
        this.name = name;
    }
}
