package ai.context.util.analysis.feed;

import ai.context.feed.Feed;

import javax.swing.*;
import java.lang.reflect.Constructor;

public class FeedBuilder extends JPanel {

    private Constructor ctor;
    public FeedBuilder(String className, String name, String[] argsDescription) throws ClassNotFoundException {
        Class<?> c = Class.forName(className);
        Constructor[] allConstructors = c.getDeclaredConstructors();
        ctor = allConstructors[0];
    }

    public Feed build(){
        Class<?>[] pType  = ctor.getParameterTypes();
        for (int i = 0; i < pType.length; i++) {
            System.out.println(pType[i]);
        }
        return null;
    }

    public static void main(String[] args) throws ClassNotFoundException {

        //FeedBuilder builder = new FeedBuilder("ai.context.feed.transformer.series.online.StandardDeviationOnlineTransformer");
    }

}
