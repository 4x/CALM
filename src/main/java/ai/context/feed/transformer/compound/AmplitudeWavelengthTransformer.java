package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AmplitudeWavelengthTransformer extends CompoundedTransformer {

    private LinkedList<Double[]> history = new LinkedList<Double[]>();

    private Double wavelength;
    private Double amplitude;
    private double[] lastCrestCoordinates;
    private double[] lastTroughCoordinates;

    private long index = 0;

    private double nDeviations;
    private double lambda;

    private Feed rawFeed;
    private Feed stdDevFeed;

    public AmplitudeWavelengthTransformer(Feed rawFeed, Feed stdDevFeed, double nDeviations, double lambda) {
        super(new Feed[]{rawFeed, stdDevFeed});
        this.nDeviations = nDeviations;
        this.lambda = lambda;
        this.rawFeed = rawFeed;
        this.stdDevFeed = stdDevFeed;
    }

    @Override
    protected Object getOutput(Object input) {

        List<Object> data = (List<Object>) input;
        Double raw = (Double) data.get(0);
        Double stdev = (Double) data.get(1);
        if (stdev == null || raw == null) {
            return null;
        }
        history.add(new Double[]{raw, stdev});

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
                    if (expectingCrest && Math.abs(snapshot[0] - start) > (nDeviations * snapshot[1])) {
                        found = true;
                    }
                } else {
                    if (!expectingCrest && Math.abs(snapshot[0] - start) > (nDeviations * snapshot[1])) {
                        found = true;
                    }
                }
            } else {
                if (snapshot[0] > last) {
                    if (Math.abs(highest - snapshot[0]) > (nDeviations * snapshot[1])) {
                        confirmed = true;
                        break;
                    }
                } else {
                    if (Math.abs(snapshot[0] - lowest) > (nDeviations * snapshot[1])) {
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
                history.poll();
            }
        }

        double[] result = new double[4];
        if (wavelength != null) {
            result[0] = Math.log(wavelength);
            result[1] = Math.log(amplitude);

            result[2] = Math.log(((history.size() + index) - lastCrestCoordinates[0]) / wavelength);
            result[3] = Math.log(((history.size() + index) - lastTroughCoordinates[0]) / wavelength);
        }

        return result;
    }

    @Override
    public Feed getCopy() {
        return new AmplitudeWavelengthTransformer(rawFeed.getCopy(), stdDevFeed.getCopy(), nDeviations, lambda);
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        String desciption = padding + "[" + startIndex + "] Amplitude-Wavelength transformer with nDeviations: " + nDeviations + " and Lambda: " + lambda + " giving: \n";

        desciption += padding + " [" + startIndex + "] Wavelength\n";
        startIndex++;
        desciption += padding + " [" + startIndex + "] Amplitude\n";
        startIndex++;
        desciption += padding + " [" + startIndex + "] Dist last Crest\n";
        startIndex++;
        desciption += padding + " [" + startIndex + "] Dist last trough";

        desciption += padding + " for feed: " + rawFeed.getDescription(startIndex, "") + ", using Standard Deviation feed: " + stdDevFeed.getDescription(startIndex, "");

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
        list.add(stdDevFeed.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 4;
    }
}
