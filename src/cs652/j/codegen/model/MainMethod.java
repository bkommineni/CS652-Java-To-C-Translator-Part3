package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class MainMethod extends MethodDef {

    public MainMethod(FuncName funcName, String returnType) {
        super(funcName, returnType);
    }

    @Override
    public void addArg(VarDef varDef) {
        super.addArg(varDef);
    }

    @Override
    public void addBody(Stat stat) {
        super.addBody(stat);
    }
}
