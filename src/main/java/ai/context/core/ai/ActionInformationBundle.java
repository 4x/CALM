package ai.context.core.ai;

import java.util.Map;
import java.util.TreeMap;

public class ActionInformationBundle {

    public final TreeMap<Integer, Double> distribution;
    public final Map<String, AdditionalStateActionInformation> actionInformationMap;

    public ActionInformationBundle(TreeMap<Integer, Double> distribution, Map<String, AdditionalStateActionInformation> actionInformationMap) {
        this.distribution = distribution;
        this.actionInformationMap = actionInformationMap;
    }
}
