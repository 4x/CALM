package ai.context.util.analysis;

import ai.context.util.mathematics.Operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class MarketMakerAnalyser {

    private TreeMap<Object, Stats> stats = new TreeMap<>();
    private LinkedList<O_MMP> orders = new LinkedList<>();

    double capital = 5000;
    int lastDay = -1;
    int lastMonth = -1;
    int nDay = 0;
    int nMonth = 0;
    double tradeToCapRatio = 20;

    double cost = 0.00007;
    double costPerMillion = 0;
    HashSet<Integer> tradeDays = new HashSet<>();

    public static void main(String[] args){
        MarketMakerAnalyser analyser = new MarketMakerAnalyser();
        analyser.load();
    }

    public void load() {
        BufferedReader br = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        SimpleDateFormat formatOutput = new SimpleDateFormat("yyyy.MM");

        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        formatOutput.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {
            String file = "/opt/dev/tmp/nohup.out_G1";
            file = "/opt/dev/tmp/nohup.out";

            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                try{
                    //2010.04.28 20:30:03.467 P: 0.00194 OPEN: 1.32022 CLOSE: 1.31828  NET: 14.19567 SHORT TIMEOUT

                    if (sCurrentLine.contains("OPEN") && !sCurrentLine.contains("Mean")  && !sCurrentLine.contains("INFO")  && !sCurrentLine.contains("SP")) {

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

                        double targetPnL = Operations.round(Math.max(Math.abs(attr.get("targetHigh")), Math.abs(attr.get("targetLow"))), 4);
                        long advised = Long.parseLong(sCurrentLine.split("=")[1].split(",")[0]);

                        O_MMP order = new O_MMP();
                        order.dir = 1;
                        if(dir.equals("SHORT")){
                            order.dir = -1;
                        }
                        order.change = change;
                        order.timeOpened = advised;
                        order.timeClosed = date.getTime();
                        orders.add(order);

                        trimOrdersWhereClosedBefore(advised - 7200000);

                        double[] stats = getStatsFromOrders(-1, advised);
                        int nShort = (int) stats[0];
                        double shortPnl = stats[1];

                        stats = getStatsFromOrders(1, advised);
                        int nLong = (int) stats[0];
                        double longPnl = stats[1];

                        stats = getStatsFromOrders(0, advised);
                        int nOrders = (int) stats[0];
                        double pnl = stats[1];

                        double reversePnL = longPnl - shortPnl;
                        int nDir = nLong - nShort;



                        Integer[] hoursToTrade = new Integer[]{8,9,10,13,14,15,16,17,18,19};
                        Set<Integer> hours = new HashSet<Integer>(Arrays.<Integer>asList(hoursToTrade));

                        double dU = Double.parseDouble(attr.get("dU_8").toString());
                        double dD = Double.parseDouble(attr.get("dD_8").toString());

                        double dP = Double.parseDouble(attr.get("d199").toString());
                        double dP10 = Double.parseDouble(attr.get("d10").toString());
                        double dP20 = Double.parseDouble(attr.get("d20").toString());
                        double dP50 = Double.parseDouble(attr.get("d50").toString());
                        double dP100 = Double.parseDouble(attr.get("d100").toString());

                        double ddU = dU - Double.parseDouble(attr.get("dU_4").toString());
                        double ddD = dD - Double.parseDouble(attr.get("dD_4").toString());

                        double slope = ddD;
                        double dY = (dU - dD);

                        double moment = ddU - ddD;
                        if(dir.equals("SHORT")){
                            reversePnL *= -1;
                            nDir *= -1;
                            dY *= -1;

                            dP *= -1;
                            dP10 *= -1;
                            dP20 *= -1;
                            dP50 *= -1;
                            dP100 *= -1;
                            slope = ddU;
                            moment *= -1;
                        }

                        int momentum = getLogarithmicDiscretisation(slope/moment, 0, 1);

                        if (
                                (20 * attr.get("wait").longValue())/lifeSpan < 10 &&
                                //attr.get("cred").intValue() >= 30 &&
                                attr.get("cred").intValue() < 10 &&
                                //lifeSpan/1800000 > 2 &&
                                //lifeSpan/1800000 < 17 &&
                                //cred >= credRange[0] &&
                                //cred <= credRange[1] &&
                                targetPnL > 0.0016 &&
                                targetPnL < 0.0026 &&
                                //hours.contains(startHour) &&
                                //nMonth == 3 &&
                                //nMonth > 4 &&
                                //hours.contains(new Date(advised).getHours()) &&
                                //Math.abs(pnl) > 0.001 &&
                                //Math.abs(pnl) < 0.005 &&
                                //nOrders == 0 &&
                                //date.getDay() != 1 &&
                                (dP100 < 0 || dP100 >= 0.0002) &&
                                dP <= 0.0003 &&
                                //dY < -0.0002 &&
                                //slope > -0.0011 &&
                                momentum >= 0 &&
                                momentum < 3 &&
                                true) {

                            aggregate(formatOutput.format(date), change);
                            //aggregate(new Date(advised).getHours(), change);
                            //aggregate(formatOutput.format(date) +","+ (double)(new Date(advised).getHours())/100, change);
                            //aggregate(formatOutput.format(date) +","+ ((double)(lifeSpan/1800000)/100), change);

                            //aggregate((int) (10000 *dY) , change);
                            //aggregate((int) (10000 *slope) , change);
                            //aggregate((int) (10000 *moment) , change);
                            //aggregate(momentum , change);

                            //aggregate((int) (10000 *dP100) , change);
                            //aggregate((int) (10000 *dP) , change);
                            //aggregate(nOrders, change);
                            //aggregate(nDir/10, change);

                            //aggregate((int) (1000 *pnl), change);
                            //aggregate((int) (1000 * reversePnL), change);

                            //aggregate(attr.get("cred").intValue(), change);
                            //aggregate(lifeSpan/1800000, change);
                            //aggregate(participants, change);
                            //aggregate((20 * attr.get("wait").longValue())/lifeSpan, change);
                            //aggregate(nMonth, change);
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
                            //aggregate((int)(targetPnL * 10000), change);

                            double commision = 2 * tradeToCapRatio * capital * costPerMillion / 1000000;
                            capital += (tradeToCapRatio * capital * change) - commision;

                            tradeDays.add(nDay);
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

    public void trimOrdersWhereClosedBefore(long time){
        while (orders.peekFirst().timeClosed < time){
            orders.pollFirst();
        }
    }

    public double[] getStatsFromOrders(int dir, long endTime){
        double n = 0;
        double pnl = 0;

        for(O_MMP order : orders){
            if(endTime > order.timeClosed && (dir == 0 || order.dir == dir)) {
                pnl += order.change;
                n++;
            }
        }
        return new double[]{n, pnl};
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
        System.out.println("Trades per Day: " + (int)(totalTrades/tradeDays.size()));
        System.out.println("Trades: " + (int)totalTrades);
        System.out.println("Net PNL: " + Operations.round(netPnL, 4));
        System.out.println("Green:Red: " + Operations.round(green/red, 3));
        System.out.println("Win:Lose: " + Operations.round(profits/(totalTrades - profits), 3));
        System.out.println("PnL/Trade (BP): " + Operations.round(10000 * netPnL/totalTrades, 3));
        System.out.println("End Capital: " + Operations.round(capital, 2));
    }
}

class O_MMP{
    long timeOpened;
    long timeClosed;
    int dir;
    double change;
}
