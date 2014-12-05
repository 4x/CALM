package ai.context.core.ai;

import ai.context.util.mathematics.AverageAggregator;

import java.util.Map;
import java.util.TreeMap;

public class AdditionalStateActionInformation {
    private TreeMap<Integer, AverageAggregator> data = new TreeMap();

    public AdditionalStateActionInformation() {
    }

    public AdditionalStateActionInformation(TreeMap<Integer, AverageAggregator> data) {
        this.data = data;
    }

    public void addValue(int key, double value, double weight){
        if(!data.containsKey(key)){
            data.put(key, new AverageAggregator());
        }
        data.get(key).adjust(value, weight);
    }

    public AdditionalStateActionInformation merge(AdditionalStateActionInformation partner) {
        TreeMap<Integer, AverageAggregator> dataMerged = new TreeMap<>();

        for (Map.Entry<Integer, AverageAggregator> entry : data.entrySet()) {
            dataMerged.put(entry.getKey(), new AverageAggregator(entry.getValue().getSum(), entry.getValue().getCount()));
        }

        for (Map.Entry<Integer, AverageAggregator> entry : partner.getData().entrySet()) {
            if(!data.containsKey(entry.getKey())) {
                dataMerged.put(entry.getKey(), new AverageAggregator(entry.getValue().getSum(), entry.getValue().getCount()));
            } else {
                dataMerged.get(entry.getKey()).adjust(entry.getValue().getSum(), entry.getValue().getCount());
            }
        }

        return new AdditionalStateActionInformation(dataMerged);
    }

    public TreeMap<Integer,AverageAggregator> getData() {
        return data;
    }

    public void setData(TreeMap<Integer, AverageAggregator> data) {
        this.data = data;
    }

    public void incorporate(AdditionalStateActionInformation partner, double weight) {
        for (Map.Entry<Integer, AverageAggregator> entry : partner.getData().entrySet()) {
            if(!data.containsKey(entry.getKey())) {
                data.put(entry.getKey(), new AverageAggregator(entry.getValue().getSum() * weight, entry.getValue().getCount() * weight));
            } else {
                data.get(entry.getKey()).adjust(entry.getValue().getSum() * weight, entry.getValue().getCount() * weight);
            }
        }
    }
}
