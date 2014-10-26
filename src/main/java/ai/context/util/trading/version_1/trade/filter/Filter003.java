package ai.context.util.trading.version_1.trade.filter;

import ai.context.util.trading.version_1.MarketMakerPosition;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class Filter003 implements TradeFilter{
    @Override
    public boolean pass(MarketMakerPosition advice) {

        long wait = (Long)advice.attributes.get("wait");
        long lifeSpan = (Long)advice.attributes.get("timeSpan");

        long plannedExitHour = (advice.getGoodTillTime() % 3600000)/3600000;
        long executeHour = (System.currentTimeMillis() % 3600000)/3600000;
        double cred = (Double)advice.attributes.get("cred");

        double dU = (Double)advice.attributes.get("dU_8");
        double dD = (Double)advice.attributes.get("dD_8");
        double ddU = dU - (Double)advice.attributes.get("dU_4");
        double ddD = dD - (Double)advice.attributes.get("dD_4");
        double slope = ddD;
        double moment = ddU - ddD;

        if(advice.isHasOpenedWithShort()){
            slope = ddU;
            moment *= -1;
        }
        int momentum = getLogarithmicDiscretisation(slope/moment, 0, 1);

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

        if (wait/60000 >= 3 &&
                wait/60000 < 30 &&
                cred >= 5 &&
                cred < 60 &&
                lifeSpan/1800000 >= 3 &&
                plannedExitHour > 1 &&
                plannedExitHour < 23 &&
                executeHour <= 19 &&
                momentum >= -1 &&
                momentum <= 9 &&
                priceMovementParameter_2 >= -9 &&
                priceMovementParameter_2 <= 9 &&
                Math.abs(priceMovementParameter_4) <= 1 &&
                priceMovementParameter_5 >= -5){
            return true;
        }

        return false;
    }
}
