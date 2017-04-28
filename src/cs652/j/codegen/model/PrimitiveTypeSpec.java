package cs652.j.codegen.model;

import com.sun.codemodel.internal.JType;
import org.antlr.symtab.Type;
import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class PrimitiveTypeSpec extends TypeSpec {
    public Type type;
    public PrimitiveTypeSpec(Type type) {
        this.type = type;
    }
}
