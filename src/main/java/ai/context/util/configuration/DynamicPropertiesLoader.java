package ai.context.util.configuration;

import ai.context.util.trading.PositionEngine;
import ai.context.util.trading.PositionFactory;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

import static ai.context.util.common.FileUtils.getMD5Checksum;

public class DynamicPropertiesLoader {

    private static String positionEngineChecksum = "";
    private static String positionFactoryConfigChecksum = "";
    private static String learnerServiceConfigCheckSum = "";

    private static String folder  = "";

    public static void start(String folder){
        DynamicPropertiesLoader.folder = folder + "/";
        TimerTask checkForUpdates = new TimerTask() {
            @Override
            public void run() {
                try{
                    loadPositionFactoryConf();
                    loadPositionFactoryEngine();
                    loadLearnerServiceConf();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer();
        timer.schedule(checkForUpdates, 1000, 1000);
    }

    private static void loadPositionFactoryEngine(){
        if(new File(folder + "PositionEngineImplementation.class").exists()){
            try {
                String checksum = getMD5Checksum(folder + "PositionEngineImplementation.class");

                if(!positionEngineChecksum.equals(checksum)){
                    positionEngineChecksum = checksum;

                    PositionFactory.setEngine((PositionEngine) ClassLoader.getSystemClassLoader().loadClass("ai.context.util.trading.PositionEngineImplementation").newInstance());

                    System.out.println("Position Engine changed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void loadLearnerServiceConf() throws Exception {
        if(!learnerServiceConfigCheckSum.equals(getMD5Checksum(folder + "LearnerService.conf")))
        {
            learnerServiceConfigCheckSum = getMD5Checksum(folder + "LearnerService.conf");
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(folder + "LearnerService.conf");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null){
                String[] parts = line.split(":");

                if(parts[0].equals("recencyBias")){
                    PropertiesHolder.recencyBias = Double.parseDouble(parts[1]);
                }
                else if(parts[0].equals("tolerance")){
                    PropertiesHolder.tolerance = Double.parseDouble(parts[1]);
                }
                else if(parts[0].equals("maxPopulation")){
                    PropertiesHolder.maxPopulation = Integer.parseInt(parts[1]);
                }
                else if(parts[0].equals("copulaToUniversal")){
                    PropertiesHolder.copulaToUniversal = Double.parseDouble(parts[1]);
                }
                else if(parts[0].equals("toleranceSearch")){
                    PropertiesHolder.toleranceSearch = Integer.parseInt(parts[1]);
                }
            }
            System.out.println("Learner Service configuration changed");
        }
    }

    private static void loadPositionFactoryConf() throws Exception {
        if(!positionFactoryConfigChecksum.equals(getMD5Checksum(folder + "PositionFactory.conf")))
        {
            positionFactoryConfigChecksum = getMD5Checksum(folder + "PositionFactory.conf");
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(folder + "PositionFactory.conf");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            try {
                while ((line = reader.readLine()) != null){
                    String[] parts = line.split(":");

                    if(parts[0].equals("tradeToCapRatio")){
                        PositionFactory.setTradeToCapRatio(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("leverage")){
                        PositionFactory.setLeverage(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("amount")){
                        PositionFactory.setAmount(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("cost")){
                        PositionFactory.setCost(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("rewardRiskRatio")){
                        PositionFactory.setRewardRiskRatio(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("minTakeProfit")){
                        PositionFactory.setMinTakeProfit(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("minProbFraction")){
                        PositionFactory.setMinProbFraction(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("verticalRisk")){
                        if(parts[1].equals("TRUE")){
                            PositionFactory.setVerticalRisk(true);
                        }
                        else{
                            PositionFactory.setVerticalRisk(false);
                        }
                    }
                    else if(parts[0].equals("minTakeProfitVertical")){
                        PositionFactory.setMinTakeProfitVertical(Double.parseDouble(parts[1]));
                    }
                    else if(parts[0].equals("timeSpan")){
                        PositionFactory.setTimeSpan(Long.parseLong(parts[1]));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Position Factory configuration changed");
        }
    }
}
