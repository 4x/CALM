package ai.context.util.analysis;

import ai.context.util.mathematics.Operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;

public class FXAnalyser {

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
    private TreeMap<Double, Long> dist = new TreeMap<>();

    public static void main(String[] args){
        FXAnalyser analyser = new FXAnalyser();
        analyser.load();
    }

    public void load() {
        BufferedReader br = null;

        try {
            String file = "/opt/dev/data/feeds/EURUSD.csv";
            br = new BufferedReader(new FileReader(file));
            String sCurrentLine = br.readLine();
            while ((sCurrentLine = br.readLine()) != null) {
                String[] parts = sCurrentLine.split(",");
                double open = Double.parseDouble(parts[1]);
                double high = Double.parseDouble(parts[2]);
                double low = Double.parseDouble(parts[3]);
                double close = Double.parseDouble(parts[4]);
                double vol = Double.parseDouble(parts[5]);

                double spread = Operations.round(high - low, 4);
                if(!dist.containsKey(spread)){
                    dist.put(spread, 0L);
                }

                dist.put(spread, dist.get(spread) + 1);
            }

            for(Map.Entry<Double, Long> entry : dist.entrySet()){
                System.out.println(entry.getKey() + "," + entry.getValue());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
