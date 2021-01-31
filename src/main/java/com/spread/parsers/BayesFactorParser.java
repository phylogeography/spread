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
import com.spread.utils.ParsersUtils;

import lombok.Setter;
import lombok.Getter;

public class BayesFactorParser {

    private static final String BAYES_FACTOR = "bayesFactor";
    private static final String POSTERIOR_PROBABILITY = "posteriorProbability";

    @Setter
    private String logFilename;
    @Setter
    private Double burninPercent;
    @Setter
    private String locationsFilename;
    @Setter
    private Integer numberLocations;

    private final Double poissonPriorMean = Math.log(2);;
    private Integer poissonPriorOffset;

    class BayesFactor {

        @Getter
        private String from;
        @Getter
        private String to;
        @Getter
        private Double bayesFactor;
        @Getter
        private Double posteriorProbability;

        BayesFactor(String from,
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

    public BayesFactorParser(String logFilename,
                             Double burninPercent,
                             String locationsFilename
                             ) {

        this.logFilename = logFilename;
        this.burninPercent = burninPercent;
        this.locationsFilename = locationsFilename;

    }

    public BayesFactorParser(String logFilename,
                             Double burninPercent,
                             Integer numberLocations
                             ) {

        this.logFilename = logFilename;
        this.burninPercent = burninPercent;
        this.numberLocations = numberLocations;

    }

    public String parse() throws IOException, SpreadException {

        Double[][] indicators = new LogParser(this.logFilename, this.burninPercent).parseIndicators();

        System.out.println("Imported log file");

        LinkedList<Location> locationsList = null;
        if (this.locationsFilename != null) {
            locationsList = new DiscreteLocationsParser(this.locationsFilename, false)
                .parseLocations();
        } else if (this.numberLocations != null) {
            locationsList = this.generateDummyLocations(this.numberLocations);
        } else {
            throw new SpreadException ("Specify one of: numberLocations or locationsFilename");
        }

        int numberLocations = locationsList.size();
        this.poissonPriorOffset = numberLocations - 1;
        int nrow = indicators.length;
        int ncol = indicators[0].length;

        Boolean symmetrical = null;
        if (ncol == numberLocations * (numberLocations - 1)) {
            symmetrical = false;
        } else if (ncol == (numberLocations * (numberLocations - 1)) / 2) {
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
            qk = (poissonPriorMean + poissonPriorOffset) / ((numberLocations * (numberLocations - 1)) / 2);
        } else {
            qk = (poissonPriorMean + poissonPriorOffset) / ((numberLocations * (numberLocations - 1)) / 1);
        }

        double priorOdds = qk / (1 - qk);
        double[] pk = getColumnMeans(indicators);

        LinkedList<Double> bayesFactors = new LinkedList<Double>();
        LinkedList<Double> posteriorProbabilities = new LinkedList<Double>();
        for (int row = 0; row < pk.length; row++) {

            double bf = (pk[row] / (1 - pk[row])) / priorOdds;

            if (bf == Double.POSITIVE_INFINITY) {
                bf = ((pk[row] - (double) (1.0 / nrow)) / (1 - (pk[row] - (double) (1.0 / nrow)))) / priorOdds;
                System.out.println("Correcting for infinite bf: " + bf);
            }

            bayesFactors.add(bf);
            posteriorProbabilities.add(pk[row]);
        }

        System.out.println("Calculated Bayes Factors");

        LinkedList<String> from = new LinkedList<String>();
        LinkedList<String> to = new LinkedList<String>();

        String[] locations = new String[locationsList.size()];
        int ii = 0;
        for (Location location : locationsList) {
            locations[ii] = location.getId();
            ii++;
        }

        for (int row = 0; row < numberLocations - 1; row++) {
            String[] subset = this.subset(locations, row, numberLocations - row);
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

        // Location dummy;
        LinkedList<Line> linesList = new LinkedList<Line>();
        for (int i = 0; i < bayesFactors.size(); i++) {

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

        System.out.println("Parsed points and lines");

        // collect attributes from lines
        HashMap<String, Attribute> branchAttributesMap = new HashMap<String, Attribute>();

        for (Line line : linesList) {

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
                } // END: key check
            } // END: attributes loop

        } // END: lines loop

        LinkedList<Attribute> uniqueLineAttributes = new LinkedList<Attribute> (branchAttributesMap.values());

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

        SpreadData spreadData = new SpreadData(null,
                                               new AxisAttributes(xCoordinate.getId(),
                                                                  yCoordinate.getId()),
                                               uniqueLineAttributes,
                                               uniquePointAttributes,
                                               null,
                                               locationsList,
                                               layersList);

        LinkedList<BayesFactor> bayesFactorsData = new LinkedList<BayesFactor>();
        for (int i = 0; i < bayesFactors.size(); i++) {
            bayesFactorsData.add(new BayesFactor(from.get(i),
                                                 to.get(i),
                                                 bayesFactors.get(i),
                                                 posteriorProbabilities.get(i)));
        }

        Object pair = new Object[] {bayesFactorsData, spreadData};
        return new GsonBuilder().create().toJson(pair);
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

        Attribute xCoordinate = new Attribute("xCoordinate", xCoordinateRange);
        Attribute yCoordinate = new Attribute("yCoordinate", yCoordinateRange);

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
     * Generates locations distributed uniformly in a circle
     */
    private LinkedList<Location> generateDummyLocations(int numberLocations) {

        double radius = 1000;

        Double centroidLatitude = 0.0;
        Double centroidLongitude = 0.0;

        double dLongitude = Math.toDegrees((radius / ParsersUtils.EARTH_RADIUS));
        double dLatitude = dLongitude
            / Math.cos(Math.toRadians(centroidLatitude));

        LinkedList<Location> locationsList = new LinkedList<Location>();
        for (int i = 0; i < numberLocations; i++) {

            String locationId = "location" + (i + 1);

            double theta = 2.0 * Math.PI * (i / (double) numberLocations);
            Double cLatitude = centroidLatitude
                + (dLongitude * Math.cos(theta));
            Double cLongitude = centroidLongitude
                + (dLatitude * Math.sin(theta));

            locationsList.add(new Location (locationId, new Coordinate(cLatitude, cLongitude)));
        }

        return locationsList;
    }

}
