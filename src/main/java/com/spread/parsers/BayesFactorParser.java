package com.spread.parsers;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.spread.data.Location;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;
import com.spread.data.primitive.Coordinate;
import com.spread.exceptions.SpreadException;
import com.spread.utils.ParsersUtils;

import lombok.Setter;

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






        return "";
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
