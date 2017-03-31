package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

/**
 * Created by bharu on 3/23/17.
 */
public class CallStat extends Stat {
    @ModelElement public MethodCall methodCall;

    public CallStat(MethodCall methodCall) {
        this.methodCall = methodCall;
    }
}
