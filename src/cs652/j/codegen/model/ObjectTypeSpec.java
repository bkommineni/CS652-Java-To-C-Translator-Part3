package cs652.j.codegen.model;

import org.antlr.symtab.Type;
import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class ObjectTypeSpec extends TypeSpec {
    public Type type;

    public ObjectTypeSpec(Type type) {
        this.type = type;
    }
}
