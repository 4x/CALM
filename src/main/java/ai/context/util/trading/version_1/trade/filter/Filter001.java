package ai.context.util.trading.version_1.trade.filter;

import ai.context.util.trading.version_1.MarketMakerPosition;

public class Filter001 implements TradeFilter{
    @Override
    public boolean pass(MarketMakerPosition advice) {

        long wait = (Long)advice.attributes.get("wait");
        double cred = (Double)advice.attributes.get("cred");

        double targetHigh = (Double)advice.attributes.get("targetHigh");
        double targetLow = (Double)advice.attributes.get("targetLow");
        double targetPnL = targetHigh + targetLow;
        double minAmp = Math.min(targetHigh, targetLow);


        double dU = (Double)advice.attributes.get("dU_8");
        double dD = (Double)advice.attributes.get("dD_8");
        double ddU = dU - (Double)advice.attributes.get("dU_4");
        double ddD = dD - (Double)advice.attributes.get("dD_4");
        double slope = ddD;
        if(advice.isHasOpenedWithShort()){
            slope = ddU;
        }

        double dP = (Double)advice.attributes.get("d199");
        double dP10 = (Double)advice.attributes.get("d10");
        double dP20 = (Double)advice.attributes.get("d20");
        double dP50 = (Double)advice.attributes.get("d50");
        double dP100 = (Double)advice.attributes.get("d100");

        double priceMovementParameter_1 = (int) (10000 * dP50 * 10000 *dP20);
        double priceMovementParameter_2 = (int) (10000 * dP100 * 10000 *dP20);
        double priceMovementParameter_3 = (int) (10000 * dP * 10000 *dP10);
        double priceMovementParameter_4 = (int) (10000 * dP50 * 10000 *dP10);
        double priceMovementParameter_5 = (int) (10000 * dP100 * 10000 *dP10);
        double priceMovementParameter_6 = (int) (10000 * dP * 10000 *dP50);

        if (wait/60000 > 3 &&
            wait/300000 <= 12 &&
            cred > 55 &&
            cred < 85 &&
            targetPnL > 0.0011 &&
            targetPnL <= 0.0020 &&
            minAmp >= -0.0001 &&
            minAmp < 0 &&
            slope <= 0.0025 &&
            priceMovementParameter_1 >= -1 &&
            priceMovementParameter_1 <= 8 &&
            priceMovementParameter_2 >= -6 &&
            priceMovementParameter_2 <= 2 &&
            priceMovementParameter_4 >= -1 &&
            priceMovementParameter_5 <= 3 &&
            priceMovementParameter_4 >= -13 &&
            priceMovementParameter_4 <= 6){
            return true;
        }

        return false;
    }
}
