package ai.context.util.analysis;

import ai.context.util.mathematics.Operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PnLAnalyser {

    private TreeMap<Object, Stats> stats = new TreeMap<>();
    TreeMap<String, Order> orders = new TreeMap<>();

    double capital = 5000;
    String lastDay = "";
    String lastMonth = "";
    int nDay = 0;
    int nMonth = 0;
    double tradeToCapRatio = 10;
    double[] credRange = new double[]{1, 2};
    Integer[] hoursToTrade = new Integer[]{6,7,8,9,10,14,15,16,17,18,19,20,21,22};
    double rebate = 0.0000;
    double sourceCharge = 0.00020;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM-dd HH:mm");

    boolean marketMakerAnalysis = true;

    public static void main(String[] args){
        PnLAnalyser analyser = new PnLAnalyser();
        analyser.load();
    }

    public void load() {
        BufferedReader br = null;

        try {
            long tStart = format.parse("2005-Jan-13 06:00").getTime();
            String file = "/opt/dev/tmp/nohup.out";
            //String file = "/opt/dev/tmp/2008-2011_logs.txt";

            int useConfigChange = -1;
            int configChange = 0;

            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            Set<Integer> hours = new HashSet<Integer>(Arrays.<Integer>asList(hoursToTrade));
            while ((sCurrentLine = br.readLine()) != null) {
                if(sCurrentLine.contains("Position Factory configuration changed")){
                    configChange++;
                    System.out.print(configChange);
                }
                if (sCurrentLine.contains("CHANGE") && !sCurrentLine.contains("Mean")) {
                    String[] parts = sCurrentLine.split(" ");

                    int dateIndex = 9;
                    int changePart = 1;
                    int dirPart = 16;
                    int lifeSpanPart = 19;
                    int targetPnlPart = 18;
                    if(marketMakerAnalysis){
                        changePart = 2;
                        dateIndex = 15;
                        dirPart = 1;
                        lifeSpanPart = 14;
                        targetPnlPart = 12;
                    }

                    Double change = Double.parseDouble(parts[changePart]);
                    String dir = parts[dirPart];
                    long lifeSpan = Long.parseLong(parts[lifeSpanPart]);
                    Double targetPnL = Double.parseDouble(parts[targetPnlPart].split("Mean")[0]);

                    long span = lifeSpan;
                    String state = "NORMAL";
                    double cred = 0;
                    int participants = 0;
                    if(!marketMakerAnalysis){
                        span = Long.parseLong(parts[17].substring(0, parts[17].indexOf('s')));
                        state = parts[15].substring(0, parts[15].length() - 1);
                        cred = Double.parseDouble(parts[6]);
                        participants = Integer.parseInt(parts[8]);
                    }

                    String day = parts[dateIndex];
                    String month = parts[dateIndex + 1];
                    long date = Long.parseLong(parts[dateIndex + 2]);
                    long year = Long.parseLong(parts[dateIndex + 5]);
                    long hour = Long.parseLong(parts[dateIndex + 3].split(":")[0]);
                    long min = Long.parseLong(parts[dateIndex + 3].split(":")[1]);
                    long t = format.parse(year + "-" + month + "-" + parts[dateIndex + 2] + " " + parts[dateIndex + 3]).getTime();

                    if(t >= tStart && (useConfigChange + 1 == configChange || useConfigChange == -1)){
                        long endTime = hour * 60 + min;
                        long startTime = (endTime - span / 60) % (24 * 60);
                        if (startTime < 0) {
                            startTime = (24 * 60) + startTime;
                        }

                        String closing = parts[parts.length - 1];

                        if (!day.equals(lastDay)) {
                            lastDay = day;
                            nDay++;
                        }

                        if (!month.equals(lastMonth)) {
                            lastMonth = month;
                            nMonth++;
                        }

                        int changeClass = (int) (Math.abs(targetPnL) * 1000);

                        int startHour = (int) (startTime / 60);
                        int credClass = (int)(cred);
                        change += rebate;

                        if (    //lifeSpan/3600000 == 2 &&
                                //cred >= credRange[0] &&
                                //cred <= credRange[1] &&
                                //targetPnL >= 0.0015 &&
                                //targetPnL < 0.002 &&
                                //hours.contains(startHour) &&
                                //lifeSpan/3600000 > 17 &&
                                true) {

                            aggregate(lifeSpan/3600000, change);
                            //aggregate(participants, change);
                            //aggregate((int)(100*cred/participants), change);
                            //aggregate(nMonth, change);
                            //aggregate(startHour, change);
                            //aggregate(changeClass, change);
                            //aggregate(hour, change);
                            //aggregate(nDay, change);
                            //aggregate(day, change);
                            //aggregate(closing, change);
                            //aggregate(span, change);
                            //aggregate(credClass, change);
                            //aggregate(targetPnL, change);
                            //aggregate((int)(targetPnL * 2000), change);

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

                            capital += (tradeToCapRatio * capital * change);
                            //System.out.println(Operations.round(capital, 2));
                        }
                    }
                }
            }
            printStats();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }


        /*reset();
        for (Order order : orders.values()) {
            String closing = order.getClosing();
            double cred = order.getCred();
            double targetPnL = order.getTargetPnL();
            double change = order.getChange() - (sourceCharge - rebate);
            int startHour = order.getStartHour();
            String month = order.getMonth() + "";
            String day = order.getDay() + "";
            if (!month.equals(lastMonth)) {
                lastMonth = month;
                nMonth++;
            }

            if (!day.equals(lastDay)) {
                lastDay = day;
                nDay++;
            }

            if (//true ||
                    cred >= credThreshold
                    && targetPnL > 0.001
                    && targetPnL < 0.002
                    && ((startHour > 4 && startHour < 9) || (startHour > 14 && startHour < 17))
            ) {
                aggregate(nMonth, change);
                capital += (tradeToCapRatio * capital * change);
            }
        }
        printStats();*/
    }

    public void aggregate(Object id, double change){
        if(!stats.containsKey(id)){
            stats.put(id, new Stats());
        }
        stats.get(id).add(change);
    }

    public void reset(){
        capital = 5000;
        lastDay = "";
        lastMonth = "";
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

class Order{

    static SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
    int merged = 1;

    Double change;
    Double pnl;
    String state;
    String dir;
    Double cred;
    Long span;
    Double targetPnL;

    Long endTime;
    Long startTime;
    String closing;

    Order(Double change, Double pnl, String state, String dir, Double cred, Long span, Double targetPnL, String day, String month, Long date, Long year, Long hour, Long min, String closing) {
        this.change = change;
        this.pnl = pnl;
        this.state = state;
        this.dir = dir;
        this.cred = cred;
        this.span = span;
        this.targetPnL = targetPnL;
        this.closing = closing;

        try {

            String dateString = date + "";
            if(dateString.length() == 1){
                dateString = "0" + date;
            }

            String hourString = hour + "";
            if(hourString.length() == 1){
                hourString = "0" + hour;
            }

            String minString = min + "";
            if(minString.length() == 1){
                minString = "0" + min;
            }

            this.endTime = format.parse(year + "-" + month + "-" + dateString + " " + hourString + ":" + minString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.startTime = endTime - span * 1000;
    }

    public Double getChange() {
        return change;
    }

    public Double getPnl() {
        return pnl;
    }

    public String getState() {
        return state;
    }

    public String getDir() {
        return dir;
    }

    public Double getCred() {
        return cred;
    }

    public Long getSpan() {
        return span;
    }

    public Double getTargetPnL() {
        return targetPnL;
    }

    public Long getEndTime() {
        return endTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public String getClosing() {
        return closing;
    }

    public void merge(Order order){
        double cred = (this.cred*merged + order.getCred());
        merged++;
        cred /= merged;
        //System.out.println(this.cred + " -> " + cred + "[" + order.getCred() + "]");
        this.cred = cred;
    }

    public String getID(){
        return startTime + "_" + dir;
    }

    public int getStartHour(){
        return new Date(startTime).getHours();
    }

    public int getMonth(){
        return new Date(startTime).getMonth();
    }

    public Object getDay() {
        return new Date(startTime).getDay();
    }
}