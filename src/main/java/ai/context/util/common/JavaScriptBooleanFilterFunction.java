package ai.context.util.common;

import org.mozilla.javascript.Context;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class JavaScriptBooleanFilterFunction implements Filter{

    private ScriptEngineManager manager = new ScriptEngineManager();
    private ScriptEngine engine = manager.getEngineByName("JavaScript");
    private Invocable inv = (Invocable) engine;
    private String functionName;

    @Override
    public boolean pass(Object o) {
        if(functionName == null){
            return true;
        }
        try{
            return (boolean) Context.jsToJava(inv.invokeFunction(functionName, o), boolean.class);
        }catch (Exception e){
            e.printStackTrace();
            functionName = null;
            return false;
        }
    }

    public void setFunction(String functionName){
        this.functionName = functionName;
    }

    public ScriptEngine getEngine() {
        return engine;
    }
}
