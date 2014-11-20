package ai.context.util.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeuronAnalyser {
    private String address = "http://hyophorbe-associates.com:8056";

    public static void main(String[] args){
       NeuronAnalyser analyser = new NeuronAnalyser();

       analyser.analyseScores();
    }

    public void analyseScores(){
        try {
            URL url = new URL(address + "/info?REQ_TYPE=ALL");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine = in.readLine();

            Pattern p = Pattern.compile("\"Neuron ([0-9]*)\", \"score\" : (0.[0-9]*)");
            Matcher m = p.matcher(inputLine);

            while(m.find()){
                int nID = Integer.parseInt(m.group(1));
                double score = Double.parseDouble(m.group(2));

                System.out.println(nID + "," + score);
            }

            p = Pattern.compile("\"source\": ([0-9]*), \"target\": ([0-9]*)");
            m = p.matcher(inputLine);

            while(m.find()){
                int s = Integer.parseInt(m.group(1)) + 1;
                int t = Integer.parseInt(m.group(2)) + 1;

                //System.out.println(s + " -> " + t);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void analyseMerges(){
        try {
            URL url = new URL(address + "/info?REQ_TYPE=ALL");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine = in.readLine();

            Pattern p = Pattern.compile("\"Neuron ([0-9]*)\", \"score\" : (0.[0-9]*)");
            Matcher m = p.matcher(inputLine);

            while(m.find()){
                int nID = Integer.parseInt(m.group(1));
                double score = Double.parseDouble(m.group(2));

                System.out.println(nID + "," + score);
            }

            p = Pattern.compile("\"source\": ([0-9]*), \"target\": ([0-9]*)");
            m = p.matcher(inputLine);

            while(m.find()){
                int s = Integer.parseInt(m.group(1)) + 1;
                int t = Integer.parseInt(m.group(2)) + 1;

                //System.out.println(s + " -> " + t);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
