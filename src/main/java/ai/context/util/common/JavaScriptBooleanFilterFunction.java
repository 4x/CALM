package ai.context.util.common;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JavaScriptBooleanFilterFunction implements Filter{

    private Function fct;
    private Context context = Context.enter();
    private ScriptableObject scope = context.initStandardObjects();
    private Scriptable that = context.newObject(scope);

    @Override
    public boolean pass(Object o) {
        if(fct == null){
            return true;
        }
        try{
            Object result = fct.call(context, scope, that, new Object[] {o});
            return (boolean) Context.jsToJava(result, boolean.class);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public void setScript(String script){
        fct = context.compileFunction(scope, script, "script", 1, null);
    }
}
