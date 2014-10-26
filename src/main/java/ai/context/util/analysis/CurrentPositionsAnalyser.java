package ai.context.util.analysis;

import ai.context.util.mathematics.Operations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.TreeMap;

public class CurrentPositionsAnalyser {

    public static void main(String[] args){
        int nAdvice = Integer.parseInt(getOutput("cluster.getMarketMakerPositions().size()"));
        System.out.println(nAdvice);

        for(int i = 0; i < nAdvice; i++){
            System.out.println("Advice "  + i);
            String cmd = "cluster.getMarketMakerPositions()";
            boolean open = Boolean.valueOf(getOutput(cmd + "[" + i + "].isOpen()"));
            boolean closed = Boolean.valueOf(getOutput(cmd + "[" + i + "].isClosed()"));
            System.out.println("OPEN: " + open);
            System.out.println("CLOSED: " + closed);

            long goodTill = Long.valueOf(getOutput(cmd + "[" + i + "].getGoodTillTime()"));
            System.out.println("GOOD TILL: " + goodTill);

            if(open && !closed){
                String[] parts = getOutput(cmd + "[" + i + "].attributes").split(" ");
                TreeMap<String, Double> attr = new TreeMap<>();
                for(int iAttr = 0; iAttr < parts.length; iAttr++){
                    String[] e = parts[iAttr].split("=");
                    String p = e[0];
                    Double v = Operations.round(Double.valueOf(e[1].substring(0, e[1].length() - 1)), 5);
                    attr.put(p, v);
                }
                System.out.println(attr);
            }
        }
    }

    public static String address = "http://hyophorbe-associates.com";
    public static String port = "8056";

    public static String getOutput(String cmd){
        String output = "";
        try {
            URL url = new URL(address + ":" + port + "/scripting?CMD=out.print(" + cmd + ")");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            output = in.readLine();
            String out;
            while((out = in.readLine()) != null){
                output += "\n" + out;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }
}
