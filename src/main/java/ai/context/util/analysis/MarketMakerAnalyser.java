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
    double tradeToCapRatio = 5;

    double cost = 0.00007;
    //double cost = 0.000036;
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


        SimpleDateFormat formatInterested = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        String[] interestDates = {"20070111",
                "20080104",
                "20080201",
                "20080307",
                "20080404",
                "20080502",
                "20080606",
                "20080703",
                "20080801",
                "20080905",
                "20081003",
                "20081107",
                "20081205",
                "20090109",
                "20090206",
                "20090306",
                "20090403",
                "20090508",
                "20090605",
                "20090702",
                "20090807",
                "20090904",
                "20091002",
                "20091106",
                "20091204",
                "20100108",
                "20100205",
                "20100305",
                "20100402",
                "20100507",
                "20100604",
                "20100702",
                "20100806",
                "20100903",
                "20101008",
                "20101105",
                "20101203",
                "20110107",
                "20110204",
                "20110304",
                "20110401",
                "20110506",
                "20110603",
                "20110708",
                "20110805",
                "20110902",
                "20111007",
                "20111104",
                "20111202",
                "20120106",
                "20120203",
                "20120309",
                "20120406",
                "20120504",
                "20120601",
                "20120706",
                "20120803",
                "20120907",
                "20121005",
                "20121102",
                "20121207",
                "20130104",
                "20130201",
                "20130308",
                "20130405",
                "20130503",
                "20130607"};

        Set<String> interestDatesSet = new HashSet<>(Arrays.asList(interestDates));

        TreeMap<Long, Set<String>> calendar = new TreeMap<>();

        try {
            BufferedReader brCal = new BufferedReader(new FileReader("/opt/dev/data/feeds/Calendar.csv"));
            String sCurrentLine;
            while ((sCurrentLine = brCal.readLine()) != null) {
                String[] parts = sCurrentLine.split(",");
                long t = formatInterested.parse(parts[0]).getTime();
                String event = parts[2] + "," + parts[1];
                if(!calendar.containsKey(t)) {
                    calendar.put(t, new HashSet<String>());
                }
                calendar.get(t).add(event);
            }
        } catch (Exception e){
            e.printStackTrace();
        }


        String[] hoursAndSeasonNotToExecute =
                {"false_0",
                "false_2",
                "false_3",
                "false_4",
                "false_8",
                "false_9",
                "false_14",
                "false_15",
                "false_17",
                "false_21",
                "true_0",
                "true_1",
                "true_3",
                "true_7",
                "true_8",
                "true_11",
                "true_13",
                "true_14",
                "true_18",
                "true_19",
                "true_20",
                "true_21"};

        Integer[] hoursNotToTrade = new Integer[]{0, 21, 22, 23};
        Integer[] hoursNotToExit = new Integer[]{0, 1, 23};
        Integer[] hoursNotToExecute = new Integer[]{1, 2, 8, 9, 14, 15, 16, 20, 21};
        Set<Integer> hoursN = new HashSet<Integer>(Arrays.<Integer>asList(hoursNotToTrade));
        Set<Integer> hoursNExit = new HashSet<Integer>(Arrays.<Integer>asList(hoursNotToExit));
        Set<Integer> hoursNExecute = new HashSet<Integer>(Arrays.<Integer>asList(hoursNotToExecute));

        String lastOrder = "";

        Set<String> hoursAndSeasonNExecute = new HashSet<String>(Arrays.<String>asList(hoursAndSeasonNotToExecute));
        try {
            String file = "/opt/dev/tmp/nohup.out_t8";
            file = "/opt/dev/tmp/nohup.out";

            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                try{
                    //2011.06.14 09:19:41.973 P: 0.00178 OPEN: 1.44658 CLOSE: 1.4448 NET: -0.38357 SHORT NORMAL 10800000
                    // {
                    // advised=1308038400000,
                    // advisedToOpenTime=389316,
                    // avgW=217.72078174242165,
                    // avgWH=1.1526351900237664,
                    // avgWL=-2.5540717449552366,
                    // cred=268.4178012939582,
                    // d1=-2.0000000000131024E-5,
                    // d10=-9.499999999995623E-5,
                    // d100=7.500000000004725E-5,
                    // d199=3.500000000000725E-4,
                    // d20=-1.9500000000016726E-4,
                    // d5=-9.000000000014552E-5,
                    // d50=-2.0000000000131024E-5,
                    // dD_0=3.9999999999995595E-4,
                    // dD_1=7.999999999999119E-4,
                    // dD_2=0.001100000000000101,
                    // dD_3=0.0016000000000000458,
                    // dD_4=0.0020000000000000018,
                    // dD_5=0.0024999999999999467,
                    // dD_6=0.0035000000000000586,
                    // dD_7=0.0044999999999999485,
                    // dD_8=0.004999999999999893,
                    // dD_9=0.004999999999999893,
                    // dU_0=4.999999999999449E-4,
                    // dU_1=8.999999999999009E-4,
                    // dU_2=0.0013000000000000789,
                    // dU_3=0.0018000000000000238,
                    // dU_4=0.0020000000000000018,
                    // dU_5=0.0029999999999998916,
                    // dU_6=0.0035000000000000586,
                    // dU_7=0.0044999999999999485,
                    // dU_8=0.004999999999999893,
                    // dU_9=0.004999999999999893,
                    // dir=-1,
                    // goodTill=1308049200000,
                    // gradHLineA=-0.017875619061638287,
                    // gradHLineE=-0.05022424977878885,
                    // gradLLineA=-0.017799573129873455,
                    // gradLLineE=-0.05000980021768442,
                    // recovery=0.004687836116520874,
                    // recoveryRatio=0.7721297254417571,
                    // targetHigh=-7.00000000000145E-5,
                    // targetLow=0.0017699999999998273,
                    // timeSpan=10800000,
                    // wait=389316
                    // }

                    if (sCurrentLine.contains("OPEN") && !sCurrentLine.contains("Mean")  && !sCurrentLine.contains("INFO")  && !sCurrentLine.contains("SP")) {

                        if(sCurrentLine.contains("SP")){
                            sCurrentLine = sCurrentLine.substring(9, sCurrentLine.length());
                        }
                        String[] parts = sCurrentLine.split(" ");

                        Date date = format.parse(sCurrentLine.substring(0, 23));
                        if (date.getDay() != lastDay) {
                            lastDay = date.getDay();
                            nDay++;
                        }

                        if (date.getMonth() != lastMonth) {
                            lastMonth = date.getMonth();
                            nMonth++;
                        }

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
                        double targetPnL = Operations.round(attr.get("targetHigh") + attr.get("targetLow"), 5);
                        double minAmp = Operations.round(Math.min(attr.get("targetHigh"), attr.get("targetLow")), 5);
                        double maxAmp = Operations.round(Math.max(attr.get("targetHigh"), attr.get("targetLow")), 5);
                        double dAmp = maxAmp - minAmp;

                        long advised = Long.parseLong(sCurrentLine.split("=")[1].split(",")[0]);
                        Date dAdvised = new Date(advised);
                        Date dPlannedExit = new Date(advised + lifeSpan);
                        Date dExecuted = new Date(advised + attr.get("wait").longValue());

                        int hExec = (int)((dExecuted.getTime() % 86400000)/3600000);

                        String executeHourAndSeason = (dExecuted.getMonth() > 2 && dExecuted.getMonth() < 9) + "_" + hExec;

                        long tOpen = date.getTime() - dExecuted.getTime();
                        long tLive = date.getTime() - dAdvised.getTime();

                        O_MMP order = new O_MMP();
                        order.dir = 1;
                        if(dir.equals("SHORT")){
                            order.dir = -1;
                        }
                        order.change = change;
                        order.timeOpened = dExecuted.getTime();
                        order.timeClosed = date.getTime();
                        //orders.add(order);


                        double avgW = attr.get("avgW");
                        double avgWH  = attr.get("avgWH");
                        double avgWL  = attr.get("avgWL");

                        double diffAvgW = avgWH - avgWL;

                        double gradHLineA = attr.get("gradHLineA");
                        double gradLLineA = attr.get("gradLLineA");
                        double gradHLineE = attr.get("gradHLineE");
                        double gradLLineE = attr.get("gradLLineE");

                        double diffA = gradHLineA - gradLLineA;
                        double diffE = gradHLineE - gradLLineE;

                        double diffHAE = gradHLineA - gradHLineE;
                        double diffLAE = gradLLineA - gradLLineE;

                        double diffHLAE = gradHLineA - gradLLineE;
                        double diffHLEA = gradHLineE - gradLLineA;

                        int gradParam = (int)((gradHLineE/gradHLineA + gradLLineA/gradLLineE));

                        int avgWHLParam = (int)(10*Math.max(avgWH, avgWL));

                        trimOrdersWhereClosedBefore(advised - 3600000);

                        double[] stats = getStatsFromOrders(0, advised, 1800000L);
                        int opennedWithin = (int) stats[2];
                        int closedBy = (int) stats[1];

                        stats = getStatsFromOrders(-1, advised, 1800000L);
                        int nShort = (int) stats[0];
                        double shortPnl = stats[1];

                        stats = getStatsFromOrders(1, advised, 1800000L);
                        int nLong = (int) stats[0];
                        double longPnl = stats[1];

                        stats = getStatsFromOrders(0, advised, 1800000L);
                        int nOrders = (int) stats[0];
                        double pnl = stats[1];

                        double reversePnL = longPnl - shortPnl;
                        int nDir = nLong - nShort;


                        double dU = Double.parseDouble(attr.get("dU_8").toString());
                        double dD = Double.parseDouble(attr.get("dD_8").toString());

                        double dP = Double.parseDouble(attr.get("d199").toString());
                        double dP10 = Double.parseDouble(attr.get("d10").toString());
                        double dP20 = Double.parseDouble(attr.get("d20").toString());
                        double dP50 = Double.parseDouble(attr.get("d50").toString());
                        double dP100 = Double.parseDouble(attr.get("d100").toString());

                        double ddU = dU - Double.parseDouble(attr.get("dU_3").toString());
                        double ddD = dD - Double.parseDouble(attr.get("dD_3").toString());

                        double slope = ddD;
                        double dY = (dU - dD);

                        double moment = ddU - ddD;
                        /*if(dir.equals("SHORT")){
                            //reversePnL *= -1;
                            //nDir *= -1;
                            dY *= -1;

                            dP *= -1;
                            dP10 *= -1;
                            dP20 *= -1;
                            dP50 *= -1;
                            dP100 *= -1;
                            slope = ddU;
                            moment *= -1;
                        }*/

                        double dPAvg = Operations.round((dP + dP10 + dP100 + dP20 + dP50)/5, 4);
                        double dPstdd = Operations.round(Math.sqrt((Math.pow(dP - dPAvg, 2) +
                                Math.pow(dP - dPAvg, 2) +
                                Math.pow(dP - dPAvg, 2) +
                                Math.pow(dP - dPAvg, 2) +
                                Math.pow(dP - dPAvg, 2)) / 5), 4);

                        int startHour = new Date(advised).getHours();
                        int momentum = getLogarithmicDiscretisation(slope/moment, 0, 0.01);
                        int slopeL = getLogarithmicDiscretisation(10000 * slope, 0, 1);

                        int recoveryRatio = (int) (10 * attr.get("recoveryRatio"));
                        if(recoveryRatio > 30){
                            recoveryRatio = 100;
                        }

                        double priceMovementParameter_1 = (int) (10000 * dP50 * 10000 *dP20);
                        double priceMovementParameter_2 = (int) (10000 * dP100 * 10000 *dP20);
                        double priceMovementParameter_3 = (int) (10000 * dP * 10000 *dP10);
                        double priceMovementParameter_4 = (int) (10000 * dP50 * 10000 *dP10);
                        double priceMovementParameter_5 = (int) (10000 * dP100 * 10000 *dP10);
                        double priceMovementParameter_6 = (int) (10000 * dP * 10000 *dP50);

                        String interestedDateStrTmp = "2012.01,2012.08,2012.09,2012.11,2013.03,2014.06,2014.11,2014.12";
                        HashSet<String> interestedDateStr = new HashSet<>(Arrays.asList(interestedDateStrTmp.split(",")));
                        String dateStr = formatOutput.format(date);

                        if (
                            //true ||
                                //date.getYear() > 111 &&
                            //dir.equals("SHORT") &&
                            //((dir.equals("LONG") && momentum >= 0 && momentum <= 6) || dir.equals("SHORT")) &&
                                //interestDatesSet.contains(formatInterested.format(date)) &&
                            //interestedDateStr.contains(dateStr) &&
                            //attr.get("recovery") >= 0.0020 &&
                            //attr.get("recovery") < 0.0050 &&
                            //(100 * attr.get("wait").longValue())/lifeSpan >= 1 &&
                            //(50 * attr.get("wait").longValue())/lifeSpan > 0 &&
                            (20 * attr.get("wait").longValue())/lifeSpan < 6  &&
                                    //hExec != 8 &&
                            //attr.get("wait").longValue()/60000 >= 6 &&
                            attr.get("wait").longValue()/60000 < 80 &&
                            //!(attr.get("cred").intValue() < 27 && attr.get("cred").intValue() > 1) &&
                            //attr.get("cred").intValue() >= 100 &&
                            attr.get("cred").intValue() < 400 &&
                            lifeSpan/1800000 >= 4 &&
                            //lifeSpan/1800000 <= 8 &&
                            //targetPnL >= 0.0014 &&
                            targetPnL < 0.0020 &&
                            minAmp >= 0.0000 &&
                            //minAmp != -0.00001 &&
                            minAmp < 0.00008 &&
                            //maxAmp > 0.0012 &&
                            //dAmp >= 0.0011 &&
                            //hours.contains(startHour) &&
                            //(nMonth == 3 || nMonth == 6) &&
                            //(nMonth == 3 || nMonth == 12 || nMonth == 13 || nMonth == 18) &&
                            //nMonth > 46 &&
                            //(nMonth > 18) &&
                            //nMonth == 10 &&
                            //hours.contains(new Date(advised).getHours()) &&
                            //!hoursN.contains(dAdvised.getHours()) &&
                            //dAdvised.getHours() < 16 &&
                            //!hoursNExit.contains(dPlannedExit.getHours()) &&
                            //dExecuted.getHours() <= 19 &&
                            //!hoursNExecute.contains(dExecuted.getHours()) &&
                            //!hoursNExecute.contains(hExec) &&
                            //!hoursAndSeasonNExecute.contains(executeHourAndSeason) &&
                            //dPlannedExit.getHours() > 1 &&
                            //dPlannedExit.getHours() < 21 &&
                            //dExecuted.getHours() > 0 &&
                            //dExecuted.getHours() != 10 &&
                            //dExecuted.getHours() != 11 &&
                            //dExecuted.getHours() <= 19 &&
                            //dAdvised.getHours() < 18 &&
                            //startHour > 8 &&
                            //Math.abs(pnl) > 0.001 &&
                            //Math.abs(pnl) < 0.005 &&
                            //nOrders == 0 &&
                            //date.getDay() != 1 &&
                            //dP10 > 0.0000 &&
                            //Math.abs(dP50) < 0.0005 &&
                            //dP20 <= 0.0002 &&
                            //dP100 >= -0.0010 &&
                            //dP100 < 0.0006 &&
                            //dP <= 0.0005 &&
                            //dP >= -0.0006 &&
                            //dY <= 0 &&
                            //slope >= 0.0020 &&
                            //slope <= 0.0031 &&
                            //moment <= 0.0005 &&
                            //momentum != -1 &&
                                    //(momentum >= 6 || momentum <= -6 || momentum == 0) &&
                                    //(momentum == 0 || (Math.abs(momentum) > 6 && Math.abs(momentum) <= 8)) &&
                            //priceMovementParameter_1 >= -3 &&
                            //priceMovementParameter_1 <= 4 &&
                            //priceMovementParameter_2 >= -8 &&
                            //priceMovementParameter_2 <= 8 &&
                            //priceMovementParameter_3 >= -6 &&
                            //priceMovementParameter_3 <= 10 &&
                            //Math.abs(priceMovementParameter_4) <= 5 &&
                            //priceMovementParameter_4 >= -3 &&
                            //priceMovementParameter_4 <= 10 &&
                            //priceMovementParameter_5 != -1 &&
                            //priceMovementParameter_6 < 50 &&
                                    //dPstdd > 0.0001 &&
                                    //dPstdd < 0.0005 &&
                            //momentum > -1 &&
                            //tOpen/600000 < 6 &&
                            //dExecuted.getDay() != 5
                            //dAdvised.getHours() != 15 &&
                            //!(dExecuted.getDay() + "_" + (dExecuted.getHours() > 11)).equals("1_false") &&
                            //(attr.get("recoveryRatio") >= 0.8 || attr.get("recoveryRatio") < 0.6) &&
                            //attr.get("recoveryRatio") >= 0.9 &&
                            attr.get("recovery") >= 0.0020 &&
                            attr.get("recovery") < 0.0049 &&
                            recoveryRatio < 31 &&
                            avgW >= 10 &&
                            avgW < 260 &&
                            //Math.abs(diffA) >= 2E-7 &&
                            //Math.abs(diffA) >= 6E-8 &&
                            diffE >= -2E-7 && diffE < 10E-7 &&
                            //(int)((gradLLineE + gradHLineE)*500000) != -1 &&
                            //(int)((gradLLineA + gradHLineA)*500000) > -8 &&
                                    (int)(avgWH + avgWL) >= -2 &&
                                    (int)(avgWH + avgWL) <= 0 &&
                                    //(int)(avgWH + avgWL) == 0 &&
                                    //(dir.equals("LONG") && avgWL < 3) || (dir.equals("SHORT") && avgWH < 3) &&
                            //(gradParam < 13 || gradParam > 19) &&
                            gradParam != 0 &&
                            //gradParam < 8 &&
                                    (int)diffAvgW != 0 &&
                            //avgWHLParam > 0 &&
                            //avgWHLParam < 29 &&
                                    //Math.abs((int)(diffE*10000000)) != 3 &&
                                    //Math.abs((int)(diffE*10000000)) != 4 &&
                                    //(int)(diffA*10000000) != -7 &&
                                    //(int)(diffA*10000000) != -6 &&
                                    //(int)(diffA*10000000) != -5 &&

                                    //(Math.abs((int)(diffE*10000000)) == 3 || Math.abs((int)(diffE*10000000)) == 4) &&
                                    //((int)(diffA*10000000) == -7 || (int)(diffA*10000000) == -6 || (int)(diffA*10000000) == -5) &&
                            //!((int)(diffE*100) == 0 || (int)(diffE*100) == -1 ) &&
                            //(int)(gradHLineE*200) != 0 &&
                                    (int)(gradHLineE*1000000) >= -3 && (int)(gradHLineE*1000000) < 3 &&
                                    //(int)(gradLLineE*1000000) >= -3 && (int)(gradLLineE*1000000) < 3 &&
                                    (int)(gradLLineE*1000000) != -1 &&
                            //(int)(priceMovementParameter_4 * priceMovementParameter_5) == 0 &&
                                    (int)(10*Math.max(avgWH, avgWL)) >= -6 &&
                                    (int)(10*Math.max(avgWH, avgWL)) < 29 &&
                                    (int)(10*Math.min(avgWH, avgWL)) >= -25 &&
                            true) {

                            lastOrder = sCurrentLine;
                            orders.add(order);

                            /*for(Set<String> events : calendar.headMap(dExecuted.getTime() + 2 * 3600000).tailMap(dExecuted.getTime() - 2 * 3600000).values()){
                                for(String event : events){
                                    aggregate(event, change);
                                }
                            }*/
                            aggregate(dateStr, change);
                            //aggregate(dateStr +" "+dir, change);
                            //aggregate(hExec, change);
                            //aggregate(dir, change);
                            //aggregate(formatOutput.format(date) + " " + dir, change);
                            //aggregate(formatOutput.format(date) +","+ (double)(new Date(advised).getHours())/100, change);
                            //aggregate(formatOutput.format(date) +","+ ((double)(lifeSpan/1800000)/100), change);
                            //aggregate(opennedWithin, change);
                            //aggregate((int) (10000 *dY) , change);
                            //aggregate((int) (10000 *slope) , change);
                            //aggregate(slopeL , change);
                            //aggregate((int) (10000 *moment) , change);
                            //aggregate(momentum , change);
                            //aggregate((int)(100000 * minAmp) , change);
                            //aggregate((int)(10000 * maxAmp) , change);
                            //aggregate((int)(10000 * dAmp) , change);

                            //aggregate((int) (10000 *dP100) , change);
                            //aggregate((int) (10000 *dP50) , change);
                            //aggregate((int) (10000 *dP20) , change);
                            //aggregate((int) (10000 *dP10) , change);
                            //aggregate((int) (10000 *dP) , change);

                            //aggregate((int) (10000 * dP50 * 10000 *dP20) , change);

                            //aggregate((int)(priceMovementParameter_6) , change);
                            //aggregate((int)(priceMovementParameter_2) , change);
                            //aggregate((int)(priceMovementParameter_5) , change);
                            //aggregate((int)(priceMovementParameter_4 * priceMovementParameter_5) , change);
                            //aggregate((int)(priceMovementParameter_5) , change);
                            //aggregate((int)(priceMovementParameter_6/10) , change);
                            //aggregate(dPstdd, change);

                            //aggregate((int)(pnl * 1000), change);
                            //aggregate(nDir/10, change);

                            //aggregate((int) (1000 *pnl), change);
                            //aggregate((int) (1000 * reversePnL), change);

                            //aggregate((attr.get("cred")).intValue()/10, change);
                            //aggregate(formatOutput.format(date), (attr.get("cred")));
                            //aggregate(date.getYear() + " " + (lifeSpan/1800000), change);
                            //aggregate(lifeSpan/1800000, change);
                            //aggregate(tOpen/600000, change);
                            //aggregate(tLive/1800000, change);

                            //aggregate(attr.get("wait").longValue()/600000, change);
                            //aggregate((20 * attr.get("wait").longValue())/lifeSpan, change);
                            //aggregate(nMonth, change);
                            //aggregate(date.getTime() / (7 * 86400000), change);
                            //aggregate(1900 + date.getYear(), change);
                            //aggregate(dAdvised.getHours(), change);
                            //aggregate(dExecuted.getDay() + "_" + (dExecuted.getHours() > 11), change);
                            //aggregate(dPlannedExit.getDay() + "_" + (dPlannedExit.getHours() > 11), change);
                            //aggregate(dAdvised.getDay(), change);
                            //aggregate(dPlannedExit.getHours(), change);
                            //aggregate(dPlannedExit.getDay(), change);
                            //aggregate(dExecuted.getHours(), change);
                            //aggregate((dExecuted.getMonth() > 2 && dExecuted.getMonth() < 9) + "," + dExecuted.getHours(), change);
                            //aggregate((dExecuted.getMonth() > 2 && dExecuted.getMonth() < 9) + "_" + hExec, change);
                            //aggregate(changeClass, change);
                            //aggregate(date.getHours(), change);
                            //aggregate(nDay, change);
                            //aggregate(dir, change);
                            //aggregate(date.getDay(), change);
                            //aggregate(dAdvised.getDay(), change);
                            //aggregate(closing, change);
                            //aggregate(span, change);
                            //aggregate(credClass, change);
                            //aggregate(date.getYear() + " " + (int)(targetPnL * 10000), change);
                            //aggregate((int)(targetPnL * 10000), change);
                            //aggregate((int)(attr.get("recovery") * 10000 ), change);
                            //aggregate(recoveryRatio, change);
                            //aggregate((int)(attr.get("recoveryRatio") / (attr.get("recovery") * 100)), change);
                            //aggregate((int)(avgW/10), change);
                            //aggregate(getLogarithmicDiscretisation(avgW/attr.get("cred"), 0, 0.01), change);
                            //aggregate((int)(avgWH + avgWL), change);
                            //aggregate((int)(diffE*10000000), change);
                            //aggregate((int)(diffA*10000000), change);
                            //aggregate((int)((gradLLineE + gradHLineE)*1000000) * order.dir, change);
                            //aggregate((int)((gradHLineE/gradHLineA + gradLLineA/gradLLineE)), change);
                            //aggregate(gradParam, change);
                            //aggregate((int)(gradHLineE*1000000), change);
                            //aggregate((int)(diffLAE*100), change);
                            //aggregate((int)(10*Math.min(avgWH, avgWL)), change);
                            //aggregate(dir, change);
                            //aggregate((int)(10*avgWH), change);
                            //aggregate((int)(10*avgWL), change);
                            //aggregate((int)(diffAvgW), change);

                            double commision = 2 * tradeToCapRatio * capital * costPerMillion / 1000000;
                            capital += (tradeToCapRatio * capital * change) - commision;

                            tradeDays.add(nDay);
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    if(e instanceof ClassCastException){
                        System.exit(1);
                    }
                }
            }
            printStats();

            System.out.println(lastOrder);
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
        while (!orders.isEmpty() && orders.peekFirst().timeClosed < time){
            orders.pollFirst();
        }
    }

    public double[] getStatsFromOrders(int dir, long endTime, long opennedWithin){
        double nOpenned = 0;
        double nClosed = 0;
        double pnl = 0;

        for(O_MMP order : orders){
            if(endTime > order.timeClosed && (dir == 0 || order.dir == dir)) {
                pnl += order.change;
                nClosed++;
            }

            if(order.timeOpened > endTime - opennedWithin){
                nOpenned++;
            }
        }
        return new double[]{nClosed, pnl, nOpenned};
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

        System.out.println("\nDays Traded: " + tradeDays.size());
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
