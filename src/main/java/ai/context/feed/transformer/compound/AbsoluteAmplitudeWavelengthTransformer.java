package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AbsoluteAmplitudeWavelengthTransformer extends CompoundedTransformer {

    private LinkedList<Double[]> history = new LinkedList<Double[]>();

    private Double wavelength;
    private Double amplitude;
    private double[] lastCrestCoordinates;
    private double[] lastTroughCoordinates;

    private long index = 0;

    private double deviation;
    private double lambda;

    private Feed rawFeed;

    private double res = 0.0001;

    public AbsoluteAmplitudeWavelengthTransformer(Feed rawFeed, double deviation, double lambda, double res) {
        super(new Feed[]{rawFeed});
        this.deviation = deviation;
        this.lambda = lambda;
        this.rawFeed = rawFeed;
        this.res = res;
    }

    @Override
    protected Object getOutput(Object input) {

        Double raw = (Double) input;
        history.add(new Double[]{raw});

        double start = history.peekFirst()[0];
        double last = start;
        double highest = start;
        double lowest = start;
        boolean expectingCrest = true;

        boolean found = false;
        boolean confirmed = false;

        int reached = 0;
        int mark = 0;
        for (Double[] snapshot : history) {

            if (snapshot[0] > highest) {
                highest = snapshot[0];
                expectingCrest = true;
                mark = reached;
            }
            if (snapshot[0] < lowest) {
                lowest = snapshot[0];
                expectingCrest = false;
                mark = reached;
            }

            if (!found) {
                if (snapshot[0] > last) {
                    if (expectingCrest && Math.abs(snapshot[0] - start) > deviation * res) {
                        found = true;
                    }
                } else {
                    if (!expectingCrest && Math.abs(snapshot[0] - start) > deviation * res) {
                        found = true;
                    }
                }
            } else {
                if (snapshot[0] > last) {
                    if (Math.abs(highest - snapshot[0]) > deviation * res) {
                        confirmed = true;
                        break;
                    }
                } else {
                    if (Math.abs(snapshot[0] - lowest) > deviation * res) {
                        confirmed = true;
                        break;
                    }
                }
            }
            last = snapshot[0];
            reached++;
        }

        if (found && confirmed) {
            index += mark;
            if (expectingCrest) {
                lastCrestCoordinates = new double[]{index, highest};
            } else {
                lastTroughCoordinates = new double[]{index, lowest};
            }

            if (lastTroughCoordinates != null && lastCrestCoordinates != null) {
                if (wavelength != null) {
                    wavelength = (1.0 - lambda) * (Math.abs(lastTroughCoordinates[0] - lastCrestCoordinates[0])) + (lambda * wavelength);
                    amplitude = (1.0 - lambda) * (Math.abs(lastCrestCoordinates[1] - lastTroughCoordinates[1])) + (lambda * amplitude);
                } else {
                    wavelength = (Math.abs(lastTroughCoordinates[0] - lastCrestCoordinates[0]));
                    amplitude = (Math.abs(lastCrestCoordinates[1] - lastTroughCoordinates[1]));
                }
            }

            for (int i = 0; i < mark; i++) {
                history.pollFirst();
            }
        }

        double[] result = new double[4];
        if (wavelength != null) {
            result[0] = Math.log(wavelength);
            result[1] = Math.log(amplitude/res);

            result[2] = Math.log(((history.size() + index) - lastCrestCoordinates[0]) / wavelength);
            result[3] = Math.log(((history.size() + index) - lastTroughCoordinates[0]) / wavelength);
        }

        return result;
    }

    @Override
    public Feed getCopy() {
        return new AbsoluteAmplitudeWavelengthTransformer(rawFeed.getCopy(), deviation, lambda, res);
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        String desciption = padding + "[" + startIndex + "] Amplitude-Wavelength transformer with deviations: " + deviation + " and Lambda: " + lambda + " giving: \n";

        desciption += padding + " [" + startIndex + "] Wavelength\n";
        startIndex++;
        desciption += padding + " [" + startIndex + "] Amplitude\n";
        startIndex++;
        desciption += padding + " [" + startIndex + "] Dist last Crest\n";
        startIndex++;
        desciption += padding + " [" + startIndex + "] Dist last trough";

        desciption += padding + " for feed: " + rawFeed.getDescription(startIndex, "");

        return desciption;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Primary Feed",
                "Feed for STDDEV of Primary Feed",
                "Number of deviation to describe peak/troughs",
                "Lamdba - how fast to update values (0 - 1)"
        };
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(rawFeed.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 4;
    }
}
