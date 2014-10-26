filter004 = function(advice) {
    var wait = advice.attributes.get("wait");
    var lifeSpan = advice.attributes.get("timeSpan");
    var plannedExitHour = (advice.getGoodTillTime() % 86400000)/3600000;

    var cred = advice.attributes.get("cred");

    var targetHigh = advice.attributes.get("targetHigh");
    var targetLow = advice.attributes.get("targetLow");
    var targetPnL = targetHigh + targetLow;
    var minAmp = Math.min(targetHigh, targetLow);

    var dP20 = advice.attributes.get("d20");
    var dP50 = advice.attributes.get("d50");

    var priceMovementParameter_1 = (10000 * dP50 * 10000 *dP20);


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

function getLogarithmicDiscretisation(value){
    value = signum(value) * (Math.log(Math.abs(value) + 1));
    return Math.round(value);
}

function signum(value){
    if(value < 0){
        return -1;
    }
    return 1;
}