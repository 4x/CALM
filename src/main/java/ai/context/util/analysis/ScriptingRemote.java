package ai.context.util.analysis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class ScriptingRemote {

    public static void main(String[] args){
        String address = "http://hyophorbe-associates.com:8056";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true){
            try{
                URL url  = new URL(address + "/scripting?CMD=" + br.readLine().replaceAll(" ", "%20")+"");
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null){
                    System.out.println(inputLine);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
}
