package ai.context.util.common;

import org.mozilla.javascript.Context;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavaScriptBooleanFilterFunction implements Filter{

    private ScriptEngineManager manager = new ScriptEngineManager();
    private ScriptEngine engine = manager.getEngineByName("JavaScript");
    private Invocable inv = (Invocable) engine;
    private String functionName = "defaultFilter";

    public JavaScriptBooleanFilterFunction() {
        String defaultFunction =
                "defaultFilter = function(advice){\n" +
                    "if((20*(advice.attributes.get(\"tNow\") - advice.getTimeAdvised()))/advice.getTimeSpan() > 4){\n" +
                        "return false;\n" +
                    "}\n" +
                    "if(advice.getTimeSpan()/1800000 < 10){\n" +
                        "return false;\n" +
                    "}\n" +
                    "return true;\n" +
                "}";

        try {
            engine.eval(defaultFunction);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean pass(Object o) {
        if(functionName == null){
            return false;
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

    public String getFunctionName() {
        return functionName;
    }

    public ScriptEngine getEngine() {
        return engine;
    }
}
