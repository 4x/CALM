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

    double cost = 0.0000;

    public static void main(String[] args){
        MarketMakerAnalyser analyser = new MarketMakerAnalyser();
        analyser.load();
    }

    public void load() {
        BufferedReader br = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");

        try {
            String file = "/opt/dev/tmp/nohup.out";

            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                try{
                    //2010.04.28 20:30:03.467 P: 0.00194 OPEN: 1.32022 CLOSE: 1.31828  NET: 14.19567 SHORT TIMEOUT

                    if (sCurrentLine.contains("OPEN") && !sCurrentLine.contains("Mean")) {
                        String[] parts = sCurrentLine.split(" ");

                        Date date = format.parse(sCurrentLine.substring(0, 23));
                        int changePart = 3;
                        int dirPart = 10;
                        int lifeSpanPart = 12;
                        Double change = Double.parseDouble(parts[changePart]) - cost;
                        String dir = parts[dirPart];
                        long lifeSpan = 0;
                        if(parts[lifeSpanPart].equals("TIMEOUT")){
                            lifeSpan = Long.parseLong(parts[lifeSpanPart + 1]);
                        }
                        else {
                            lifeSpan = Long.parseLong(parts[lifeSpanPart]);
                        }

                        String state = "NORMAL";


                        String closing = parts[parts.length - 1];

                        if (date.getDay() != lastDay) {
                            lastDay = date.getDay();
                            nDay++;
                        }

                        if (date.getMonth() != lastMonth) {
                            lastMonth = date.getMonth();
                            nMonth++;
                        }

                        if (
                            //lifeSpan/1800000 > 4 &&
                            //cred >= credRange[0] &&
                            //cred <= credRange[1] &&
                            //targetPnL >= 0.0015 &&
                            //targetPnL < 0.002 &&
                            //hours.contains(startHour) &&
                                true) {

                            //aggregate(lifeSpan/1800000, change);
                            //aggregate(participants, change);
                            //aggregate((int)(100*cred/participants), change);
                            aggregate(nMonth, change);
                            //aggregate(date.getHours(), change);
                            //aggregate(changeClass, change);
                            //aggregate(hour, change);
                            //aggregate(nDay, change);
                            //aggregate(day, change);
                            //aggregate(closing, change);
                            //aggregate(span, change);
                            //aggregate(credClass, change);
                            //aggregate(targetPnL, change);
                            //aggregate((int)(targetPnL * 2000), change);

                            capital += (tradeToCapRatio * capital * change);
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
