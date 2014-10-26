filter003 = function(advice) {
    var wait = advice.attributes.get("wait");
    var lifeSpan = advice.attributes.get("timeSpan");
    var plannedExitHour = (advice.getGoodTillTime() % 86400000)/3600000;
    var d = new Date();
    var executeHour = (d.getTime() % 86400000)/3600000;

    var cred = advice.attributes.get("cred");

    var dU = advice.attributes.get("dU_8");
    var dD = advice.attributes.get("dD_8");
    var ddU = dU - advice.attributes.get("dU_4");
    var ddD = dD - advice.attributes.get("dD_4");
    var slope = ddD;
    var moment = ddU - ddD;

    if(advice.isHasOpenedWithShort()){
        slope = ddU;
        moment *= -1;
    }
    var momentum = getLogarithmicDiscretisation(slope/moment);

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

    if (wait/60000 >= 3 &&
            wait/60000 < 30 &&
            cred >= 5 &&
            cred < 60 &&
            lifeSpan/1800000 >= 3 &&
            plannedExitHour >= 2 &&
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