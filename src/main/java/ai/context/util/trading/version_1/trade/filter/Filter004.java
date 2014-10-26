package ai.context.util.trading.version_1.trade.filter;

import ai.context.util.trading.version_1.MarketMakerPosition;

public class Filter004 implements TradeFilter{
    @Override
    public boolean pass(MarketMakerPosition advice) {

        long wait = (Long)advice.attributes.get("wait");
        double cred = (Double)advice.attributes.get("cred");
        long lifeSpan = (Long)advice.attributes.get("timeSpan");

        long plannedExitHour = (advice.getGoodTillTime() % 3600000)/3600000;
        double targetHigh = (Double)advice.attributes.get("targetHigh");
        double targetLow = (Double)advice.attributes.get("targetLow");
        double targetPnL = targetHigh + targetLow;
        double minAmp = Math.min(targetHigh, targetLow);

        double dP20 = (Double)advice.attributes.get("d20");
        double dP50 = (Double)advice.attributes.get("d50");

        double priceMovementParameter_1 = (int) (10000 * dP50 * 10000 *dP20);

        if ((20 * wait)/lifeSpan < 7 &&
            wait/60000 < 40 &&
            cred < 60 &&
            lifeSpan/1800000 >= 3 &&
            lifeSpan/1800000 <= 12 &&
            targetPnL <= 0.0016 &&
            minAmp >= -0.0001 &&
            minAmp < 0.0001 &&
            plannedExitHour > 1 &&
            priceMovementParameter_1 >= -1 &&
            priceMovementParameter_1 <= 13){
            return true;
        }

        return false;
    }
}
