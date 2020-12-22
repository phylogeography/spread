package com.spread.parsers;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import com.spread.data.Location;
import com.spread.data.primitive.Coordinate;
import com.spread.utils.ParsersUtils;
import com.spread.exceptions.SpreadException;

public class DiscreteLocationsParser {

        private static final int LATITUDE_COLUMN = 1;
        private static final int LONGITUDE_COLUMN = 2;

        private String locationsFilename;
        // private LinkedList<Location> locationsList;
        private boolean header;

        public DiscreteLocationsParser(String locationsFilename, boolean header) {

                this.locationsFilename = locationsFilename;
                this.header = header;

        }// END: Constructor

        public LinkedList<Location> parseLocations() throws IOException, SpreadException {

                LinkedList<Location> locationsList = new LinkedList<Location>();

                // create list from the coordinates file
                String[] lines = ParsersUtils.readLines(locationsFilename, ParsersUtils.HASH_COMMENT);

                if (header) {
                        lines = Arrays.copyOfRange(lines, 1, lines.length);
                }

                int nrow = lines.length;
                for (int i = 0; i < nrow; i++) {

                        // String[] line = lines[i].split("\t");
                        // TODO:test
                        String[] line = lines[i].split("\\s+");

                        if (line.length != 3) {
                                throw new SpreadException(
                                                "Incorrect number of columns in locations file. Expecting 3, found " + line.length);
                        }

                        String locationName = line[0];
                        // remove trailing spaces from
                        locationName = locationName.trim();

                        String illegalCharacter = "+";
                        if (locationName.contains(illegalCharacter)) {

                                throw new SpreadException(
                                                "Location " + locationName + " contains illegal character " + illegalCharacter);

                        }

                        Double yCoordinate = Double.valueOf(line[LATITUDE_COLUMN]);
                        Double xCoordinate = Double.valueOf(line[LONGITUDE_COLUMN]);

                        Coordinate coordinate = new Coordinate(yCoordinate, xCoordinate);

                        // create Location and add to the list of Locations
                        Location location = new Location(locationName, coordinate);
                        locationsList.add(location);

                } // END: i loop

                return locationsList;
        }// END: parseLocations

}// END: class
