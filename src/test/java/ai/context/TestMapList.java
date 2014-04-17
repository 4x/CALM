package ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestMapList {

    public static void main(String[] args){
        Map<String, String> map = new ConcurrentHashMap();
        //Map<String, String> map = new HashMap();


        for(int i = 0; i < 10000; i++){
            map.put("" + i, "" + i);
        }

        long t = System.nanoTime();

        for(int i = 0; i < 100000; i++){
            List list = new ArrayList<>(map.values());
            list.toArray(new String[list.size()]);
            //map.values().toArray(new String[map.size()]);
        }
        System.out.print((System.nanoTime() - t)/1000000.0);

    }
}
