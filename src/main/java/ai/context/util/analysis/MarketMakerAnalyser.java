package ai.context.util.analysis;

import ai.context.util.mathematics.Operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class MarketMakerAnalyser {

    private TreeMap<Object, Stats> stats = new TreeMap<>();
    TreeMap<String, Order> orders = new TreeMap<>();

    double capital = 5000;
    int lastDay = -1;
    int lastMonth = -1;
    int nDay = 0;
    int nMonth = 0;
    double tradeToCapRatio = 5;

    double cost = 0.00007;
    double costPerMillion = 0;

    double stopLoss = 0.00075;

    public static void main(String[] args){
        MarketMakerAnalyser analyser = new MarketMakerAnalyser();
        analyser.load();
    }

    public void load() {
        BufferedReader br = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        SimpleDateFormat formatOutput = new SimpleDateFormat("yyyy.MM");

        try {
            String file = "/opt/dev/tmp/nohup.out_2b";
            file = "/opt/dev/tmp/nohup.out";

            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                try{
                    //2010.04.28 20:30:03.467 P: 0.00194 OPEN: 1.32022 CLOSE: 1.31828  NET: 14.19567 SHORT TIMEOUT

                    if (sCurrentLine.contains("OPEN") && !sCurrentLine.contains("Mean")  && !sCurrentLine.contains("SP")) {

                        if(sCurrentLine.contains("SP")){
                            sCurrentLine = sCurrentLine.substring(9, sCurrentLine.length());
                        }
                        String[] parts = sCurrentLine.split(" ");

                        Date date = format.parse(sCurrentLine.substring(0, 23));
                        int changePart = 3;
                        int dirPart = 10;
                        int lifeSpanPart = 12;
                        Double change = Double.parseDouble(parts[changePart]);

                        /*change = Math.max(-stopLoss, change) - cost;*/
                        change -= cost;

                        String dir = parts[dirPart];
                        if(!(dir.equals("SHORT") || dir.equals("LONG"))){
                            dir = parts[11];
                        }
                        long lifeSpan = 0;
                        if(parts[lifeSpanPart].equals("TIMEOUT")){
                            lifeSpanPart = lifeSpanPart + 1;
                        }
                        lifeSpan = Long.parseLong(parts[lifeSpanPart]);

                        String state = "NORMAL";

                        String closing = parts[parts.length - 1];

                        TreeMap<String, Double> attr = new TreeMap<>();
                        for(int i = lifeSpanPart + 1; i < parts.length; i++){
                            String[] e = parts[i].split("=");
                            String p = e[0];
                            Double v = Double.valueOf(e[1].substring(0, e[1].length() - 1));

                            attr.put(p, v);
                        }

                        if (date.getDay() != lastDay) {
                            lastDay = date.getDay();
                            nDay++;
                        }

                        if (date.getMonth() != lastMonth) {
                            lastMonth = date.getMonth();
                            nMonth++;
                        }

                        if (
                            (20 * attr.get("wait").longValue())/lifeSpan < 4 &&
                            attr.get("cred").intValue() > 5 &&
                            //lifeSpan/1800000 == 10 &&
                            //cred >= credRange[0] &&
                            //cred <= credRange[1] &&
                            //targetPnL >= 0.0015 &&
                            //targetPnL < 0.002 &&
                            //hours.contains(startHour) &&
                            //nMonth > 5 &&
                            //nMonth < 60 &&
                                true) {

                            //aggregate((lifeSpan/1800000)+"," +formatOutput.format(date), change);
                            //aggregate(attr.get("cred").intValue(), change);
                            //aggregate(lifeSpan/1800000, change);
                            //aggregate(participants, change);
                            //aggregate((int)(100*cred/participants), change);
                            //aggregate((20 * attr.get("wait").longValue())/lifeSpan, change);
                            //aggregate(nMonth, change);
                            aggregate(formatOutput.format(date), change);
                            //aggregate(date.getTime() / (7 * 86400000), change);
                            //aggregate(date.getYear(), change);
                            //aggregate(date.getHours(), change);
                            //aggregate(changeClass, change);
                            //aggregate(date.getHours(), change);
                            //aggregate(nDay, change);
                            //aggregate(dir, change);
                            //aggregate(date.getDay(), change);
                            //aggregate(closing, change);
                            //aggregate(span, change);
                            //aggregate(credClass, change);
                            //aggregate(targetPnL, change);
                            //aggregate((int)(targetPnL * 2000), change);

                            double commision = 2 * tradeToCapRatio * capital * costPerMillion / 1000000;
                            capital += (tradeToCapRatio * capital * change) - commision;
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            printStats();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    public void aggregate(Object id, double change){
        if(!stats.containsKey(id)){
            stats.put(id, new Stats());
        }
        stats.get(id).add(change);
    }

    public void reset(){
        capital = 5000;
        lastDay = -1;
        lastMonth = -1;
        nDay = 0;
        nMonth = 0;
        tradeToCapRatio = 10;
        stats.clear();
    }

    public void printStats(){
        System.out.println("\n\nclass," + Stats.header);
        double netPnL = 0;
        double green = 0;
        double red = 0;
        double profits = 0;
        double totalTrades = 0;
        for(Map.Entry<Object, Stats> entry : stats.entrySet()){
            Stats s = entry.getValue();
            System.out.println(entry.getKey() + "," + s);
            netPnL += (s.totalProfit - s.totalLoss);
            if(s.totalProfit > s.totalLoss){
                green++;
            }
            else {
                red++;
            }
            totalTrades += s.count;
            profits += s.nProfit;
        }

        System.out.println("\nDays Traded: " + nDay);
        System.out.println("Trades: " + (int)totalTrades);
        System.out.println("Net PNL: " + Operations.round(netPnL, 4));
        System.out.println("Green:Red: " + Operations.round(green/red, 3));
        System.out.println("Win:Lose: " + Operations.round(profits/(totalTrades - profits), 3));
        System.out.println("PnL/Trade (BP): " + Operations.round(10000 * netPnL/totalTrades, 3));
        System.out.println("End Capital: " + Operations.round(capital, 2));
    }
}
