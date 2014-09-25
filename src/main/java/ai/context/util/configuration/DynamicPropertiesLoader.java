package ai.context.util.configuration;

import ai.context.util.trading.version_1.PositionEngine;
import ai.context.util.trading.version_1.PositionFactory;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

import static ai.context.util.common.FileUtils.getMD5Checksum;

public class DynamicPropertiesLoader {

    private static String globalPropertiesFile = "global.conf";

    private static String positionEngineChecksum = "";
    private static String positionFactoryConfigChecksum = "";
    private static String globalPropertiesConfigCheckSum = "";

    private static String folder = "";

    public static void start(String folder) {
        if (!folder.equals("")) {
            DynamicPropertiesLoader.folder = folder + "/";
        }
        TimerTask checkForUpdates = new TimerTask() {
            @Override
            public void run() {
                try {
                    loadPositionFactoryConf();
                    loadPositionFactoryEngine();
                    loadGlobalPropertiesConf();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer();
        timer.schedule(checkForUpdates, 1000, 1000);
    }

    private static void loadPositionFactoryEngine() {
        if (new File(folder + "PositionEngineImplementation.class").exists()) {
            try {
                String checksum = getMD5Checksum(folder + "PositionEngineImplementation.class");

                if (!positionEngineChecksum.equals(checksum)) {
                    positionEngineChecksum = checksum;

                    PositionFactory.setEngine((PositionEngine) ClassLoader.getSystemClassLoader().loadClass("ai.context.util.trading.PositionEngineImplementation").newInstance());

                    System.out.println("Position Engine changed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void loadGlobalPropertiesConf() throws Exception {
        if (new File(folder + globalPropertiesFile).exists()) {
            if (!globalPropertiesConfigCheckSum.equals(getMD5Checksum(folder + globalPropertiesFile))) {
                globalPropertiesConfigCheckSum = getMD5Checksum(folder + globalPropertiesFile);
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(folder + globalPropertiesFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");

                    if (parts[0].equals("recencyBias")) {
                        PropertiesHolder.recencyBias = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("tolerance")) {
                        PropertiesHolder.tolerance = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("maxPopulation")) {
                        PropertiesHolder.maxPopulation = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("copulaToUniversal")) {
                        PropertiesHolder.copulaToUniversal = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("toleranceSearch")) {
                        PropertiesHolder.toleranceSearch = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("dukascopyLogin")) {
                        PropertiesHolder.dukascopyLogin = parts[1];
                    } else if (parts[0].equals("dukascopyPass")) {
                        PropertiesHolder.dukascopyPass = parts[1];
                    } else if (parts[0].equals("initialSeriesOffset")) {
                        PropertiesHolder.initialSeriesOffset = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("parentsPerNeuron")) {
                        PropertiesHolder.parentsPerNeuron = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("addtionalStimuliPerNeuron")) {
                        PropertiesHolder.addtionalStimuliPerNeuron = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("coreStimuliPerNeuron")) {
                        PropertiesHolder.coreStimuliPerNeuron = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("totalNeurons")) {
                        PropertiesHolder.totalNeurons = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("generationNeurons")) {
                        PropertiesHolder.generationNeurons = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("httpPort")) {
                        PropertiesHolder.httpPort = Integer.parseInt(parts[1]);
                    } else if (parts[0].equals("normalisationOfSuggestion")) {
                        PropertiesHolder.normalisationOfSuggestion = Boolean.valueOf(parts[1]);
                    } else if (parts[0].equals("additionalPassRatio")) {
                        PropertiesHolder.additionalPassRatio = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("horizonLowerBound")) {
                        PropertiesHolder.horizonLowerBound = Long.parseLong(parts[1]);
                    } else if (parts[0].equals("horizonUpperBound")) {
                        PropertiesHolder.horizonUpperBound = Long.parseLong(parts[1]);
                    } else if (parts[0].equals("neuronLearningPeriod")) {
                        PropertiesHolder.neuronLearningPeriod = Long.parseLong(parts[1]);
                    } else if (parts[0].equals("generationLifespan")) {
                        PropertiesHolder.generationLifespan = Long.parseLong(parts[1]);
                    } else if (parts[0].equals("marketMakerConfidence")) {
                        PropertiesHolder.marketMakerConfidence = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("marketMakerLeeway")) {
                        PropertiesHolder.marketMakerLeeway = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("marketMakerAmplitude")) {
                        PropertiesHolder.marketMakerAmplitude = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("marketMakerStopLoss")) {
                        PropertiesHolder.marketMakerStopLoss = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("marketMakerBeyond")) {
                        PropertiesHolder.marketMakerBeyond = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("neuronReplacement")) {
                        PropertiesHolder.neuronReplacement = Boolean.valueOf(parts[1]);
                    } else if (parts[0].equals("tradeNormal")) {
                        PropertiesHolder.tradeNormal = Boolean.valueOf(parts[1]);
                    } else if (parts[0].equals("tradeMarketMarker")) {
                        PropertiesHolder.tradeMarketMarker = Boolean.valueOf(parts[1]);
                    } else if (parts[0].equals("startDateTime")) {
                        PropertiesHolder.startDateTime = line.substring(parts[0].length() + 1);
                    } else if (parts[0].equals("liveTrading")) {
                        PropertiesHolder.liveTrading = Boolean.valueOf(parts[1]);
                    } else if (parts[0].equals("tradeSpecial")) {
                        PropertiesHolder.tradeSpecial = Boolean.valueOf(parts[1]);
                    } else if (parts[0].equals("tradeToCreditRatio")) {
                        PropertiesHolder.tradeToCreditRatio = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("maxLeewayAmplitude")) {
                        PropertiesHolder.maxLeewayAmplitude = Double.parseDouble(parts[1]);
                    } else if (parts[0].equals("pointsToLearn")) {
                        PropertiesHolder.pointsToLearn = Long.parseLong(parts[1]);
                    } else if (parts[0].equals("timeQuantum")) {
                        PropertiesHolder.timeQuantum = Long.parseLong(parts[1]);
                    } else if (parts[0].equals("fxFolder")) {
                        PropertiesHolder.fxFolder = parts[1];
                    } else if (parts[0].equals("ticksFile")) {
                        PropertiesHolder.ticksFile = parts[1];
                    }
                }
                System.out.println("Global configuration changed: " + PropertiesHolder.getInfo());
            }
        }
    }

    private static void loadPositionFactoryConf() throws Exception {
        if (new File(folder + "PositionFactory.conf").exists()) {
            if (!positionFactoryConfigChecksum.equals(getMD5Checksum(folder + "PositionFactory.conf"))) {
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
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");

                        if (parts[0].equals("tradeToCapRatio")) {
                            PositionFactory.setTradeToCapRatio(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("leverage")) {
                            PositionFactory.setLeverage(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("amount")) {
                            PositionFactory.setAmount(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("cost")) {
                            PositionFactory.setCost(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("rewardRiskRatio")) {
                            PositionFactory.setRewardRiskRatio(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("minTakeProfit")) {
                            PositionFactory.setMinTakeProfit(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("minProbFraction")) {
                            PositionFactory.setMinProbFraction(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("verticalRisk")) {
                            if (parts[1].equals("TRUE")) {
                                PositionFactory.setVerticalRisk(true);
                            } else {
                                PositionFactory.setVerticalRisk(false);
                            }
                        } else if (parts[0].equals("minTakeProfitVertical")) {
                            PositionFactory.setMinTakeProfitVertical(Double.parseDouble(parts[1]));
                        } else if (parts[0].equals("timeSpan")) {
                            PositionFactory.setTimeSpan(Long.parseLong(parts[1]));
                        } else if (parts[0].equals("credThreshold")) {
                            PositionFactory.setCredThreshold(Double.parseDouble(parts[1]));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Position Factory configuration changed");
            }
        }
    }
}
