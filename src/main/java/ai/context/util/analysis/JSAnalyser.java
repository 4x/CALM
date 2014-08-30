package ai.context.util.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class JSAnalyser {
    public static void main(String[] args){
        String address = "http://hyophorbe-associates.com";
        String port = "8056";

        if(args.length > 0){
            address = args[0];
        }
        if(args.length > 1){
            port = args[1];
        }

        while (true){
            try{
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                String input;

                while((input=br.readLine())!=null){
                    URL url = new URL(address + ":" + port + "/scripting?CMD=" + input);
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                    String out;
                    while((out = in.readLine()) != null){
                        System.out.println(out);
                    }
                }

            }catch(IOException io){
                io.printStackTrace();
            }
        }
    }
}
