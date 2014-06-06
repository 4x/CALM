package ai.context.util.analysis;

import ai.context.util.mathematics.Operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class PnLAnalyser {

    private TreeMap<Object, Stats> stats = new TreeMap<>();
    TreeMap<String, Order> orders = new TreeMap<>();

    double capital = 5000;
    String lastDay = "";
    String lastMonth = "";
    int nDay = 0;
    int nMonth = 0;
    double tradeToCapRatio = 10;
    double credThreshold = 7;
    double rebate = 0.00010;
    double sourceCharge = 0.00020;
    boolean hasTargetLogging = true;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM-dd HH:mm");

    public static void main(String[] args){
        PnLAnalyser analyser = new PnLAnalyser();
        analyser.load();
    }

    public void load() {
        BufferedReader br = null;

        try {
            long tStart = format.parse("2008-Jan-13 06:00").getTime();
            String file = "/opt/dev/tmp/nohup.out";
            //String file = "/opt/dev/tmp/2008-2011_logs.txt";

            int useConfigChange = -1;
            int configChange = 0;

            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if(sCurrentLine.contains("Position Factory configuration changed")){
                    configChange++;
                    System.out.print(configChange);
                }
                if (sCurrentLine.startsWith("CHANGE")) {
                    String[] parts = sCurrentLine.split(" ");
                    Double change = Double.parseDouble(parts[1]);
                    Double pnl = Double.parseDouble(parts[4]);
                    String state = parts[13].substring(0, parts[13].length() - 1);
                    String dir = parts[14];
                    Double cred = Double.parseDouble(parts[6]);
                    Long span = Long.parseLong(parts[15].substring(0, parts[15].indexOf('s')));
                    Double targetPnL = Math.abs(change + 0.0002);
                    if (hasTargetLogging) {
                        targetPnL = Double.parseDouble(parts[16].split("Mean")[0]);
                    }

                    String day = parts[7];
                    String month = parts[8];
                    Long date = Long.parseLong(parts[9]);
                    Long year = Long.parseLong(parts[12].substring(0, parts[12].length() - 1));
                    Long hour = Long.parseLong(parts[10].split(":")[0]);
                    Long min = Long.parseLong(parts[10].split(":")[1]);

                    long t = format.parse(year + "-" + month + "-" + parts[9] + " " + parts[10]).getTime();
                    if(t >= tStart && (useConfigChange + 1 == configChange || useConfigChange == -1)){
                        Long endTime = hour * 60 + min;
                        Long startTime = (endTime - span / 60) % (24 * 60);
                        if (startTime < 0) {
                            startTime = (24 * 60) + startTime;
                        }

                        String closing = "NORMAL";
                        int index = 16;
                        if (hasTargetLogging) {
                            index = 17;
                        }
                        if (parts.length == 1 + index) {
                            closing = parts[index];
                        }

                        if (!day.equals(lastDay)) {
                            lastDay = day;
                            nDay++;
                        }

                        if (!month.equals(lastMonth)) {
                            lastMonth = month;
                            nMonth++;
                        }

                        int changeClass = (int) (Math.abs(targetPnL) * 1000);

                        long startHour = startTime / 60;
                        int credClass = cred.intValue();

                        Order order = new Order(change + sourceCharge, pnl, state, dir, cred, span, targetPnL, day, month, date, year, hour, min, closing);
                        if (!orders.containsKey(order.getID())) {
                            orders.put(order.getID(), order);
                        } else {
                            orders.get(order.getID()).merge(order);
                        }
                        change += rebate;

                        if (true ||
                                cred >= credThreshold
                                /*&& targetPnL > 0.0015
                                && targetPnL < 0.002
                                && ((startHour > 4 && startHour < 9) || (startHour > 14 && startHour < 17))*/
                                ) {

                            //aggregate(nMonth, change);
                            //aggregate(startHour, change);
                            //aggregate(changeClass, change);
                            //aggregate(hour, change);
                            //aggregate(nDay, change);
                            //aggregate(day, change);
                            //aggregate(closing, change);
                            //aggregate(span, change);
                            aggregate(credClass, change);
                            //aggregate(targetPnL, change);
                            //aggregate(Math.abs((int)(change * 10000)), change);

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