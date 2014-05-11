package ai.context.util.analysis;

import ai.context.util.mathematics.Operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class PnLAnalyser {

    private TreeMap<Object, Stats> stats = new TreeMap<>();
    public static void main(String[] args){
        PnLAnalyser analyser = new PnLAnalyser();
        analyser.load();
    }

    public void load(){
        BufferedReader br = null;
        double capital = 5000;
        String lastDay = "";
        String lastMonth = "";
        int nDay = 0;
        int nMonth = 0;
        double tradeToCapRatio = 10;

        try {
            //String file = "/opt/dev/tmp/nohup.out";
            String file = "/opt/dev/tmp/2008-2011_logs.txt";
            boolean hasTargetLogging = false;

            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if(sCurrentLine.startsWith("CHANGE")){
                    String[] parts = sCurrentLine.split(" ");
                    Double change = Double.parseDouble(parts[1]);
                    Double pnl = Double.parseDouble(parts[4]);
                    String state = parts[13].substring(0, parts[13].length() - 1);
                    String dir = parts[14];
                    Double cred = Double.parseDouble(parts[6]);
                    Long span = Long.parseLong(parts[15].substring(0, parts[15].indexOf('s')));
                    Double targetPnL = 0D;
                    if(hasTargetLogging){
                        targetPnL = Double.parseDouble(parts[16]);
                    }

                    String day = parts[7];
                    String month = parts[8];
                    Long date = Long.parseLong(parts[9]);
                    Long year = Long.parseLong(parts[12].substring(0, parts[12].length() - 1));
                    Long hour = Long.parseLong(parts[10].split(":")[0]);
                    Long min = Long.parseLong(parts[10].split(":")[1]);

                    Long endTime = hour*60 + min;
                    Long startTime = (endTime - span/60) % (24*60);
                    if(startTime < 0){
                        startTime = (24*60) + startTime;
                    }

                    String closing = "NORMAL";
                    int index = 16;
                    if(hasTargetLogging){
                        index = 17;
                    }
                    if(parts.length == 1 + index){
                        closing = parts[index];
                    }

                    if(!day.equals(lastDay)){
                        lastDay = day;
                        nDay++;
                    }

                    if(!month.equals(lastMonth)){
                        lastMonth = month;
                        nMonth++;
                    }

                    int changeClass = (int)(Math.abs(change + 0.0002)*1000);
                    double rebate = 0.00010;
                    long startHour = startTime/60;
                    change += rebate;
                    int credClass = (int)(cred * 10);
                    //aggregate(credClass, change);

                    if(/*true || */(closing.equals("NORMAL")
                            || closing.equals("LOCKING_PROFIT")
                            || closing.equals("FORCED")
                            || closing.equals("TIMEOUT"))
                            && cred >= 10
                            && changeClass > 0
                            && changeClass < 5
                            /*&& ((startHour > 3 && startHour < 9) || startHour == 12 || startHour == 13 || startHour == 18 || startHour == 19 || startHour == 20)*/
                            ){

                        aggregate(nMonth, change);
                        //aggregate(startHour, change);
                        //aggregate(changeClass, change);
                        //aggregate(hour, change);
                        //aggregate(nDay, change);
                        //aggregate(closing, change);
                        //aggregate(span, change);
                        //aggregate(credClass, change);

                        /*String dateString = "";
                        if(date < 10){
                            dateString += "0";
                        }
                        dateString += date;

                        String timeString = "";
                        if(startHour < 10){
                            timeString += "0";
                        }
                        timeString+= startHour + ":";
                        if(min < 10){
                            timeString += "0";
                        }
                        timeString += min;

                        System.out.print(dateString + "-" + month + "-" + year + " " + timeString + ",");
                        System.out.printf("%f\n", Operations.round(change, 4));*/
                        //System.out.println(sCurrentLine);

                        capital += (tradeToCapRatio*capital*change);
                        //System.out.println(Operations.round(capital, 2));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        System.out.println("class," + Stats.header);
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

    public void aggregate(Object id, double change){
        if(!stats.containsKey(id)){
            stats.put(id, new Stats());
        }
        stats.get(id).add(change);
    }
}

class Stats{
    static String header = "count,nProfit,nLoss,nRatio,totalProfit,totalLoss,avgProfit,avgLoss,totalRatio,pnl";
    long count = 0;
    long nProfit = 0;
    long nLoss = 0;
    double totalProfit = 0;
    double totalLoss = 0;
    TreeMap<Double, Long> changeCounts = new TreeMap<>();

    public void add(double change){
        count++;
        if(change > 0){
            nProfit++;
            totalProfit += change;
        }
        else {
            nLoss++;
            totalLoss -= change;
        }

        if(!changeCounts.containsKey(change)){
            changeCounts.put(change, 0L);
        }
        changeCounts.put(change, changeCounts.get(change) + 1);
    }


    @Override
    public String toString() {
        return count +
                "," + nProfit +
                "," + nLoss +
                "," + Operations.round((double)nProfit/(double)nLoss, 3) +
                "," + Operations.round(totalProfit, 4) +
                "," + Operations.round(totalLoss, 4) +
                "," + Operations.round(totalProfit/nProfit, 5) +
                "," + Operations.round(totalLoss/nLoss, 5) +
                "," + Operations.round(totalProfit/totalLoss, 4) +
                "," + Operations.round((totalProfit - totalLoss), 4)
                /*+ ", changeCounts=" + changeCounts +*/;
    }
}
