package ai.context.trading;

import ai.context.util.common.JavaScriptBooleanFilterFunction;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestJavaScript {
    public static void main(String[] args){

        final JavaScriptBooleanFilterFunction filterFunction = new JavaScriptBooleanFilterFunction();
        ScriptEngine engine = filterFunction.getEngine();
        engine.put("filter", filterFunction);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while(true){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    i++;

                    System.out.println(i + ": " + filterFunction.pass(i));
                }
            }
        };

        new Thread(r).start();

        while(true){
            try{
                BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
                String s = bufferRead.readLine();

                engine.eval(s);
            }
            catch(IOException e) {
                e.printStackTrace();
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }
    }
}
