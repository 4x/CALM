defaultFilter = function(advice){
	var wait = advice.attributes.get("wait");
    var cred = advice.attributes.get("cred");

    var lifeSpan = advice.attributes.get("timeSpan");
    var d = new Date();
    var executeHour = (d.getTime() % 86400000)/3600000;

    var targetHigh = advice.attributes.get("targetHigh");
    var targetLow = advice.attributes.get("targetLow");
    var targetPnL = targetHigh + targetLow;
    var minAmp = Math.min(targetHigh, targetLow);

    var dU = advice.attributes.get("dU_8");
    var dD = advice.attributes.get("dD_8");
    var ddU = dU - advice.attributes.get("dU_4");
    var ddD = dD - advice.attributes.get("dD_4");

    var dY = (dU - dD);
    var moment = ddU - ddD;

    if(advice.isHasOpenedWithShort()){
        dY *= -1;
        moment *= -1;
    }

    if (wait/60000 <= 110 &&
        cred >= 15 &&
        targetPnL <= 0.0017 &&
        minAmp >= -0.0001 &&
        minAmp < 0 &&
        lifeSpan/1800000 >= 5 &&
        lifeSpan/1800000 <= 12 &&
        dY <= 0 &&
        moment <= 0.0005 &&
        executeHour > 0 &&
        executeHour != 10 &&
        executeHour != 11 &&
        executeHour <= 21){
        return true;
    }
    return false;
}