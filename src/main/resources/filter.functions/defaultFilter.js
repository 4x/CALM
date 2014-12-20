defaultFilter = function(advice){
	var wait = advice.attributes.get("wait");
    var cred = advice.attributes.get("cred");
    var lifeSpan = advice.attributes.get("timeSpan");

    var targetHigh = advice.attributes.get("targetHigh");
    var targetLow = advice.attributes.get("targetLow");
    var targetPnL = targetHigh + targetLow;
    var minAmp = Math.min(targetHigh, targetLow);

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

    if ((100*wait/lifeSpan) >= 1 &&
        (20*wait/lifeSpan) <= 15 &&
        lifeSpan/1800000 >= 5 &&
        cred >= 5 &&
        targetPnL > 0.0011 &&
        targetPnL <= 0.0020 &&
        minAmp >= -0.0001 &&
        minAmp < 0 &&
        priceMovementParameter_1 >= -3 &&
        priceMovementParameter_1 <= 4 &&
        priceMovementParameter_2 >= -8 &&
        priceMovementParameter_2 <= 5 &&
        priceMovementParameter_3 <= 1 &&
        priceMovementParameter_4 >= -3 &&
        priceMovementParameter_4 <= 6 &&
        priceMovementParameter_5 != -1){
        return true;
    }
    return false;
}