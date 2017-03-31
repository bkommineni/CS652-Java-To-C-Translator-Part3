package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bharu on 3/23/17.
 */
public class MethodCall extends Expr {
    public String name;
    public String classname;
    @ModelElement public VarRef receiver;
    @ModelElement public TypeSpec receiverType;
    @ModelElement public FuncPtrType fptrType;
    @ModelElement public List<Expr> args = new ArrayList<>();

    public MethodCall(String name, String classname) {
        this.name = name;
        this.classname = classname;
    }

    public void setReceiver(VarRef receiver) {
        this.receiver = receiver;
    }

    public void setReceiverType(TypeSpec receiverType) {
        this.receiverType = receiverType;
    }

    public void setFptrType(FuncPtrType fptrType) {
        this.fptrType = fptrType;
    }

    public void addArg(Expr arg)
    {
        args.add(arg);
    }
}
