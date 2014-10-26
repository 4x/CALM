filter001 = function(advice){
	var wait = advice.attributes.get("wait");
    var cred = advice.attributes.get("cred");

    var targetHigh = advice.attributes.get("targetHigh");
    var targetLow = advice.attributes.get("targetLow");
    var targetPnL = targetHigh + targetLow;
    var minAmp = Math.min(targetHigh, targetLow);

    var dU = advice.attributes.get("dU_8");
    var dD = advice.attributes.get("dD_8");
    var ddU = dU - advice.attributes.get("dU_4");
    var ddD = dD - advice.attributes.get("dD_4");
    var slope = ddD;
    if(advice.isHasOpenedWithShort()){
        slope = ddU;
    }

    var dP = advice.attributes.get("d199");
    var dP10 = advice.attributes.get("d10");
    var dP20 = advice.attributes.get("d20");
    var dP50 = advice.attributes.get("d50");
    var dP100 = advice.attributes.get("d100");

    var priceMovementParameter_1 = (10000 * dP50 * 10000 *dP20);
    var priceMovementParameter_2 = (10000 * dP100 * 10000 *dP20);
    var priceMovementParameter_3 = (10000 * dP * 10000 *dP10);
    var priceMovementParameter_4 = (10000 * dP50 * 10000 *dP10);
    var priceMovementParameter_5 = (10000 * dP100 * 10000 *dP10);
    var priceMovementParameter_6 = (10000 * dP * 10000 *dP50);

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