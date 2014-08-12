package ai.context;

import ai.context.util.trading.version_1.MarketMakerPosition;
import ai.context.util.trading.version_1.MarketMakerDeciderHistorical;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TestMarketMakerDeciderHistorical {

    public static void main(String[] _args) {
        MarketMakerDeciderHistorical marketMakerDeciderHistorical = new MarketMakerDeciderHistorical("/opt/dev/data/feeds/EURUSD_UTC_Ticks_2006.01.02_2014.07.02.csv", null);

        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        //1.18463,1.18453
        try {
            long time = format.parse("2006.01.02 16:10:20").getTime();
            marketMakerDeciderHistorical.addAdvice(new MarketMakerPosition(time, 1.1845,  1.1800, 1.186,  1.17, time + 28*60*1000L));
            marketMakerDeciderHistorical.setTime(time);
            marketMakerDeciderHistorical.step();

            time = format.parse("2006.01.02 17:10:20").getTime();
            marketMakerDeciderHistorical.addAdvice(new MarketMakerPosition(time, 1.1845,  1.18435, 1.186,  1.17, time + 28*60*1000L));
            marketMakerDeciderHistorical.step();
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }
}
