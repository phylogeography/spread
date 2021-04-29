package com.spread.parsers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import com.google.gson.GsonBuilder;
import com.spread.data.Attribute;
import com.spread.data.AxisAttributes;
import com.spread.data.Layer;
import com.spread.data.Location;
import com.spread.data.SpreadData;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;
import com.spread.data.primitive.Coordinate;
import com.spread.exceptions.SpreadException;
import com.spread.progress.IProgressObserver;
import com.spread.progress.IProgressReporter;
import com.spread.utils.ParsersUtils;

import lombok.Setter;
import lombok.Getter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

public class BayesFactorParser implements IProgressReporter {

    public static final String BAYES_FACTOR = "bayesFactor";
    public static final String POSTERIOR_PROBABILITY = "posteriorProbability";

    @Setter
    private String logFilePath;
    @Setter
    private Double burnIn;
    @Setter
    private String locationsFilePath;
    @Setter
    private Integer numberOfLocations;

    private IProgressObserver progressObserver;

    public class BayesFactorParserOutput {
        public LinkedList<BayesFactor> bayesFactors;
        public SpreadData spreadData;

        BayesFactorParserOutput (LinkedList<BayesFactor> bayesFactors,
                                 SpreadData spreadData) {
            this.bayesFactors = bayesFactors;
            this.spreadData = spreadData;
        }
    }

    @EqualsAndHashCode
    @ToString(includeFieldNames=true)
    public static class BayesFactor {

        @Getter
        private String from;
        @Getter
        private String to;
        @Getter
        private Double bayesFactor;
        @Getter
        private Double posteriorProbability;

        public BayesFactor(String from,
                           String to,
                           Double bayesFactor,
                           Double posteriorProbability) {
            this.from = from;
            this.to = to;
            this.bayesFactor = bayesFactor;
            this.posteriorProbability = posteriorProbability;
        }
    }

    public BayesFactorParser() {
    }

    public BayesFactorParser(String logFilePath,
                             Double burnIn,
                             String locationsFilePath) {
        this.logFilePath = logFilePath;
        this.burnIn = burnIn;
        this.locationsFilePath = locationsFilePath;
    }

    public BayesFactorParser(String logFilePath,
                             Double burnIn,
                             Integer numberOfLocations) {
        this.logFilePath = logFilePath;
        this.burnIn = burnIn;
        this.numberOfLocations = numberOfLocations;
    }

    public String parse() throws IOException, SpreadException {

        double progress = 0;
        double progressStepSize = 0;
        this.updateProgress(progress);

        Double[][] indicators = new LogParser(this.logFilePath, this.burnIn).parseIndicators();
        LinkedList<Location> locationsList = null;

        if (this.locationsFilePath != null) {
            locationsList = new DiscreteLocationsParser(this.locationsFilePath, false)
                .parseLocations();
        } else if (this.numberOfLocations != null) {
            locationsList = this.generateDummyLocations(this.numberOfLocations);
        } else {
            throw new SpreadException ("Specify one of: numberOfLocations or locationsFilePath");
        }

        int numberOfLocations = locationsList.size();
        int poissonPriorOffset = numberOfLocations - 1;
        double poissonPriorMean = Math.log(2);
        int nrow = indicators.length;
        int ncol = indicators[0].length;

        Boolean symmetrical = null;
        if (ncol == numberOfLocations * (numberOfLocations - 1)) {
            symmetrical = false;
        } else if (ncol == (numberOfLocations * (numberOfLocations - 1)) / 2) {
            symmetrical = true;
        } else {
            int n1 = (int) ((Math.sqrt(4 * ncol + 1) + 1) / 2);
            int n2 = (int) ((Math.sqrt(8 * ncol + 1) + 1) / 2);
            throw new SpreadException("Number of rate indicators (" + ncol + ")" + " does not match the number of locations!" + " Specify "
                                      + n2 + " locations if the location exchange models is a symmetrical one, or " + n1
                                      + " for a non-symmetrical one.");
        }

        double qk = Double.NaN;
        if (symmetrical) {
            qk = (poissonPriorMean + poissonPriorOffset) / ((numberOfLocations * (numberOfLocations - 1)) / 2);
        } else {
            qk = (poissonPriorMean + poissonPriorOffset) / ((numberOfLocations * (numberOfLocations - 1)) / 1);
        }

        double priorOdds = qk / (1 - qk);
        double[] pk = getColumnMeans(indicators);
        this.updateProgress(0.1);

        LinkedList<Double> bayesFactors = new LinkedList<Double>();
        LinkedList<Double> posteriorProbabilities = new LinkedList<Double>();

        progressStepSize = 0.2 / (double) pk.length;
        for (int row = 0; row < pk.length; row++) {

            progress += progressStepSize;
            this.updateProgress(progress);

            double bf = (pk[row] / (1 - pk[row])) / priorOdds;
            // correcting for infinite bf
            if (bf == Double.POSITIVE_INFINITY) {
                bf = ((pk[row] - (double) (1.0 / nrow)) / (1 - (pk[row] - (double) (1.0 / nrow)))) / priorOdds;
            }

            bayesFactors.add(bf);
            posteriorProbabilities.add(pk[row]);
        }

        LinkedList<String> from = new LinkedList<String>();
        LinkedList<String> to = new LinkedList<String>();

        String[] locations = new String[locationsList.size()];
        int ii = 0;
        for (Location location : locationsList) {
            locations[ii] = location.getId();
            ii++;
        }

        for (int row = 0; row < numberOfLocations - 1; row++) {
            String[] subset = this.subset(locations, row, numberOfLocations - row);
            for (int i = 1; i < subset.length; i++) {
                from.add(locations[row]);
                to.add(subset[i]);
            }
        }

        if (!symmetrical) {
            from.addAll(to);
            to.addAll(from);
        }

        HashMap<Location, Point> pointsMap = new HashMap<Location, Point>();
        LinkedList<Line> linesList = new LinkedList<Line>();
        progressStepSize = 0.2 / (double) bayesFactors.size();
        for (int i = 0; i < bayesFactors.size(); i++) {

            progress += progressStepSize;
            this.updateProgress(progress);

            // from is parsed first
            Location dummy = new Location(from.get(i));
            int fromLocationIndex = Integer.MAX_VALUE;
            if (locationsList.contains(dummy)) {
                fromLocationIndex = locationsList.indexOf(dummy);
            } else {
                System.out.println("Location " + dummy.getId() + " could not be found in the locations file. Resulting file may be incomplete!");
                continue;
            }

            Location fromLocation = locationsList.get(fromLocationIndex);
            Point fromPoint = pointsMap.get(fromLocation);
            if (fromPoint == null) {
                fromPoint = createPoint(fromLocation);
                pointsMap.put(fromLocation, fromPoint);
            }

            // to is parsed second
            dummy = new Location(to.get(i));
            int toLocationIndex = Integer.MAX_VALUE;
            if (locationsList.contains(dummy)) {
                toLocationIndex = locationsList.indexOf(dummy);
            } else {
                String message = "Parent location " + dummy.getId() + " could not be found in the locations file.";
                throw new SpreadException(message);
            }

            Location toLocation = locationsList.get(toLocationIndex);
            Point toPoint = pointsMap.get(toLocation);
            if (toPoint == null) {
                toPoint = createPoint(toLocation);
                pointsMap.put(toLocation, toPoint);
            }

            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();

            Double bayesFactor = bayesFactors.get(i);
            attributes.put(BAYES_FACTOR, bayesFactor);

            Double posteriorProbability = posteriorProbabilities.get(i);
            attributes.put(POSTERIOR_PROBABILITY, posteriorProbability);

            if (!fromLocation.hasCoordinate()) {
                System.out.println("Coordinate values could not be found for the location " + fromLocation.getId()
                                   + " Resulting visualisation may be incomplete!");
                continue;
            }

            if (!toLocation.hasCoordinate()) {
                System.out.println("Coordinate values could not be found for the location " + toLocation.getId()
                                   + " Resulting visualisation may be incomplete!");
                continue;
            }

            Line line = new Line(fromPoint.getId(), //
                                 toPoint.getId(), //
                                 null, //
                                 null, //
                                 attributes //
                                 );

            linesList.add(line);
        }

        LinkedList<Point> pointsList = new LinkedList<Point>(pointsMap.values());

        // collect attributes from lines
        HashMap<String, Attribute> branchAttributesMap = new HashMap<String, Attribute>();
        progressStepSize = 0.2 / (double) linesList.size();
        for (Line line : linesList) {

            progress += progressStepSize;
            this.updateProgress(progress);
            for (Entry<String, Object> entry : line.getAttributes().entrySet()) {

                String attributeId = entry.getKey();
                Object attributeValue = entry.getValue();

                if (branchAttributesMap.containsKey(attributeId)) {

                    Attribute attribute = branchAttributesMap.get(attributeId);

                    if (attribute.getScale().equals(Attribute.ORDINAL)) {

                        attribute.getDomain().add(attributeValue);

                    } else {

                        double value = ParsersUtils.round(Double.valueOf(attributeValue.toString()), 100);

                        if (value < attribute.getRange()[Attribute.MIN_INDEX]) {
                            attribute.getRange()[Attribute.MIN_INDEX] = value;
                        } // END: min check

                        if (value > attribute.getRange()[Attribute.MAX_INDEX]) {
                            attribute.getRange()[Attribute.MAX_INDEX] = value;
                        } // END: max check

                    } // END: scale check

                } else {

                    Attribute attribute;
                    if (attributeValue instanceof Double) {
                        Double[] range = new Double[2];
                        range[Attribute.MIN_INDEX] = (Double) attributeValue;
                        range[Attribute.MAX_INDEX] = (Double) attributeValue;

                        attribute = new Attribute(attributeId, range);
                    } else {

                        HashSet<Object> domain = new HashSet<Object>();
                        domain.add(attributeValue);

                        attribute = new Attribute(attributeId, domain);
                    } // END: isNumeric check

                    branchAttributesMap.put(attributeId, attribute);
                }
            }
        }

        LinkedList<Attribute> uniqueLineAttributes = new LinkedList<Attribute>(branchAttributesMap.values());

        LinkedList<Attribute> rangeAttributes = getCoordinateRangeAttributes(locationsList);
        Attribute xCoordinate = rangeAttributes.get(ParsersUtils.X_INDEX);
        Attribute yCoordinate = rangeAttributes.get(ParsersUtils.Y_INDEX);

        LinkedList<Attribute> uniquePointAttributes = new LinkedList<Attribute>();
        uniquePointAttributes.add(xCoordinate);
        uniquePointAttributes.add(yCoordinate);

        LinkedList<Layer> layersList = new LinkedList<Layer>();

        Layer bfLayer = new Layer.Builder ()
            .withPoints (pointsList)
            .withLines (linesList)
            .build ();

        layersList.add(bfLayer);

        SpreadData spreadData = new SpreadData.Builder()
            .withAnalysisType(ParsersUtils.BAYES_FACTOR)
            .withAxisAttributes(new AxisAttributes(xCoordinate.getId(),
                                                   yCoordinate.getId()))
            .withLineAttributes(uniqueLineAttributes)
            .withPointAttributes(uniquePointAttributes)
            .withLocations(locationsList)
            .withLayers(layersList)
            .build();

        LinkedList<BayesFactor> bayesFactorsData = new LinkedList<BayesFactor>();
        progressStepSize = 0.2 / (double) bayesFactors.size();
        for (int i = 0; i < bayesFactors.size(); i++) {
            progress += progressStepSize;
            this.updateProgress(progress);
            bayesFactorsData.add(new BayesFactor(from.get(i),
                                                 to.get(i),
                                                 bayesFactors.get(i),
                                                 posteriorProbabilities.get(i)));
        }

        this.updateProgress(1.0);

        return new GsonBuilder()
            .create()
            .toJson(new BayesFactorParserOutput (bayesFactorsData, spreadData));
    }

    private LinkedList<Attribute> getCoordinateRangeAttributes(LinkedList<Location> locationsList) throws SpreadException {

        LinkedList<Attribute> coordinateRange = new LinkedList<Attribute>();

        Double[] xCoordinateRange = new Double[2];
        xCoordinateRange[Attribute.MIN_INDEX] = Double.MAX_VALUE;
        xCoordinateRange[Attribute.MAX_INDEX] = Double.MIN_VALUE;

        Double[] yCoordinateRange = new Double[2];
        yCoordinateRange[Attribute.MIN_INDEX] = Double.MAX_VALUE;
        yCoordinateRange[Attribute.MAX_INDEX] = Double.MIN_VALUE;

        for (Location location : locationsList) {

            Coordinate coordinate = location.getCoordinate();
            if (coordinate == null) {
                throw new SpreadException("Location " + location.getId()
                                          + " has no coordinates set.");
            }

            Double latitude = coordinate.getXCoordinate();
            Double longitude = coordinate.getYCoordinate();

            // update coordinates range

            if (latitude < xCoordinateRange[Attribute.MIN_INDEX]) {
                xCoordinateRange[Attribute.MIN_INDEX] = latitude;
            } // END: min check

            if (latitude > xCoordinateRange[Attribute.MAX_INDEX]) {
                xCoordinateRange[Attribute.MAX_INDEX] = latitude;
            } // END: max check

            if (longitude < yCoordinateRange[Attribute.MIN_INDEX]) {
                yCoordinateRange[Attribute.MIN_INDEX] = longitude;
            } // END: min check

            if (longitude > yCoordinateRange[Attribute.MAX_INDEX]) {
                yCoordinateRange[Attribute.MAX_INDEX] = longitude;
            } // END: max check

        }

        Attribute xCoordinate = new Attribute(ParsersUtils.X_COORDINATE, xCoordinateRange);
        Attribute yCoordinate = new Attribute(ParsersUtils.Y_COORDINATE, yCoordinateRange);

        coordinateRange.add(ParsersUtils.X_INDEX, xCoordinate);
        coordinateRange.add(ParsersUtils.Y_INDEX, yCoordinate);

        return coordinateRange;
    }

    private Point createPoint(Location location) {
        return new Point(location.getId());
    }

    private String[] subset(String line[], int start, int length) {
        String output[] = new String[length];
        System.arraycopy(line, start, output, 0, length);
        return output;
    }

    private double getColumnMean(Double a[][], int col) {
        double sum = 0;
        int nrows = a.length;
        for (int row = 0; row < nrows; row++) {
            sum += a[row][col];
        }
        return sum / nrows;
    }

    private double[] getColumnMeans(Double a[][]) {
        int ncol = a[0].length;
        double[] b = new double[ncol];
        for (int c = 0; c < ncol; c++) {
            b[c] = getColumnMean(a, c);
        }
        return b;
    }

    /*
     * Generates locations distributed uniformly on a circle with fixed radius of 1000 KM
     */
    private LinkedList<Location> generateDummyLocations(int numberOfLocations) {

        double radius = 1000;

        Double centroidLatitude = 0.0;
        Double centroidLongitude = 0.0;

        double dLongitude = Math.toDegrees((radius / ParsersUtils.EARTH_RADIUS));
        double dLatitude = dLongitude
            / Math.cos(Math.toRadians(centroidLatitude));

        LinkedList<Location> locationsList = new LinkedList<Location>();
        for (int i = 0; i < numberOfLocations; i++) {

            String locationId = "location" + (i + 1);

            double theta = 2.0 * Math.PI * (i / (double) numberOfLocations);
            Double cLatitude = centroidLatitude
                + (dLongitude * Math.cos(theta));
            Double cLongitude = centroidLongitude
                + (dLatitude * Math.sin(theta));

            locationsList.add(new Location (locationId, new Coordinate(cLatitude, cLongitude)));
        }

        return locationsList;
    }

    @Override
    public void registerProgressObserver(IProgressObserver observer) {
        this.progressObserver = observer;
    }

    @Override
    public void updateProgress(double progress) {
        if (this.progressObserver != null) {
            this.progressObserver.handleProgress(progress);
        }
    }

}
