filter006 = function(advice) {
    var wait = advice.attributes.get("wait");
    var lifeSpan = advice.attributes.get("timeSpan");
    var plannedExitHour = (advice.getGoodTillTime() % 86400000)/3600000;
    var d = new Date();
    var executeHour = parseInt((d.getTime() % 86400000)/3600000);
    var executeHourAndSeason = (d.getMonth() > 2 && d.getMonth() < 9) + "_" + executeHour;

    var cred = advice.attributes.get("cred");
    var recoveryRatio = advice.attributes.get("recoveryRatio");

    var hoursAndSeasonNotToExecute =
    {"false_0":true,
    "false_2":true,
    "false_3":true,
    "false_4":true,
    "false_8":true,
    "false_9":true,
    "false_14":true,
    "false_15":true,
    "false_17":true,
    "false_21":true,
    "true_0":true,
    "true_1":true,
    "true_3":true,
    "true_7":true,
    "true_8":true,
    "true_11":true,
    "true_13":true,
    "true_14":true,
    "true_18":true,
    "true_19":true,
    "true_20":true,
    "true_21":true};

    var targetHigh = advice.attributes.get("targetHigh");
    var targetLow = advice.attributes.get("targetLow");
    var targetPnL = targetHigh + targetLow;
    var minAmp = Math.min(targetHigh, targetLow);

    if (    (20 * wait)/lifeSpan < 13 &&
            wait/60000 < 90 &&
            !(executeHourAndSeason in hoursAndSeasonNotToExecute) &&
            minAmp >= -0.0001 &&
            minAmp < 0.0001 &&
            (recoveryRatio >= 0.8 || recoveryRatio < 0.6)){
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