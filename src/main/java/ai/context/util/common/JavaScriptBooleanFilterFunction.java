package ai.context.util.common;

import org.mozilla.javascript.Context;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

public class JavaScriptBooleanFilterFunction implements Filter{

    private ScriptEngineManager manager = new ScriptEngineManager();
    private ScriptEngine engine = manager.getEngineByName("JavaScript");
    private Invocable inv = (Invocable) engine;
    private String functionName = "defaultFilter";

    public JavaScriptBooleanFilterFunction() {
        try {
            Reader reader = new BufferedReader(new FileReader("defaultFilter.js"));
            engine.eval(reader);
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
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
