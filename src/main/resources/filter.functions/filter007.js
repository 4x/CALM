filter007 = function(advice){
	var wait = advice.attributes.get("wait");
    var cred = advice.attributes.get("cred");
    var lifeSpan = advice.attributes.get("timeSpan");

    var targetHigh = advice.attributes.get("targetHigh");
    var targetLow = advice.attributes.get("targetLow");
    var targetPnL = targetHigh + targetLow;
    var minAmp = Math.min(targetHigh, targetLow);

    var recovery = advice.attributes.get("recovery");
    var recoveryRatio = advice.attributes.get("recoveryRatio");

    var avgW = advice.attributes.get("avgW");
    var avgWH  = advice.attributes.get("avgWH");
    var avgWL  = advice.attributes.get("avgWL");

    var diffAvgW = avgWH - avgWL;

    var gradHLineA = advice.attributes.get("gradHLineA");
    var gradLLineA = advice.attributes.get("gradLLineA");
    var gradHLineE = advice.attributes.get("gradHLineE");
    var gradLLineE = advice.attributes.get("gradLLineE");

    var diffE = gradHLineE - gradLLineE;

    var intGHE = Math.floor(gradHLineE*1000000);
    var intGLE = Math.floor(gradLLineE*1000000);
    var intAvgWHL = Math.floor(avgWH + avgWL);

    var gradParam = Math.floor(gradHLineE/gradHLineA + gradLLineA/gradLLineE);

    var intAvgWHMax = Math.floor(10*Math.max(avgWH, avgWL));
    var intAvgWHMin = Math.floor(10*Math.min(avgWH, avgWL));

    if ((20 * wait)/lifeSpan < 6  &&
        wait/60000 < 80 &&
        cred < 400 &&
        lifeSpan/1800000 >= 4 &&
        targetPnL < 0.0020 &&
        minAmp >= 0.0000 &&
        minAmp < 0.00008 &&
        recovery >= 0.0020 &&
        recovery < 0.0049 &&
        recoveryRatio < 3.1 &&
        avgW >= 10 &&
        avgW < 260 &&
        diffE >= -2E-7 && diffE < 10E-7 &&
        intAvgWHL >= -2 &&
        intAvgWHL <= 0 &&
        gradParam != 0 &&
        Math.floor(diffAvgW) != 0 &&
        intGHE >= -3 && intGHE < 3 &&
        intGLE != -1 &&
        intAvgWHMax >= -6 &&
        intAvgWHMax < 29 &&
        intAvgWHMin >= -25){
        return true;
    }
    return false;
}