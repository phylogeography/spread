package com.spread.parsers;

import java.io.IOException;
import java.util.LinkedList;

import com.spread.data.Location;
import com.spread.data.primitive.Coordinate;
import com.spread.exceptions.SpreadException;
import com.spread.utils.ParsersUtils;

public class BayesFactorParser {


    private String logFilename;
    private Double burninPercent;
    private String locationsFilename;
    private Integer numberLocations;


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








        return "";
    }


    public LinkedList<Location> generateDummyLocations(int numPoints) {

        double radius = 1000;

        Double centroidLatitude = 0.0;
        Double centroidLongitude = 0.0;

        double dLongitude = Math.toDegrees((radius / ParsersUtils.EARTH_RADIUS));
        double dLatitude = dLongitude
            / Math.cos(Math.toRadians(centroidLatitude));

        LinkedList<Location> locationsList = new LinkedList<Location>();
        for (int i = 0; i < numPoints; i++) {

            String locationId = "location" + (i + 1);

            double theta = 2.0 * Math.PI * (i / (double) numPoints);
            Double cLatitude = centroidLatitude
                + (dLongitude * Math.cos(theta));
            Double cLongitude = centroidLongitude
                + (dLatitude * Math.sin(theta));

            locationsList.add(new Location (locationId, new Coordinate(cLatitude, cLongitude)));
        }

        return locationsList;
    }


}
