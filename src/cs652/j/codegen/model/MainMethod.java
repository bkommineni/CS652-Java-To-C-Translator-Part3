package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class MainMethod extends MethodDef {

    public MainMethod() {
        super();
    }

    public void addArg(VarDef varDef)
    {
        args.add(varDef);
    }

    public void setBlock(Block body)
    {
        this.body = body;
    }
}
