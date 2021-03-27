package com.spread;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import com.spread.data.Attribute;
import com.spread.data.Location;
import com.spread.data.SpreadData;
import com.spread.exceptions.SpreadException;
import com.spread.parsers.DiscreteTreeParser;
import com.spread.utils.ParsersUtils;

import org.junit.Test;

import jebl.evolution.io.ImportException;

public class DiscreteTreeParserTest {

    @Test
    public void runTest() throws IOException, ImportException, SpreadException {

        String treeFilename = "discrete/H5N1_HA_discrete_MCC.tree";
        File treeFile = new File(getClass().getClassLoader().getResource(treeFilename).getFile());

        String locationsFilename = "discrete/locationCoordinates_H5N1";
        File locationsFile = new File(getClass().getClassLoader().getResource(locationsFilename).getFile());

        String mostRecentSamplingDate = "2019/02/12";
        String locationTraitAttributeName = "states";

        DiscreteTreeParser parser = new DiscreteTreeParser(treeFile.getAbsolutePath(),
                                                           locationsFile.getAbsolutePath(),
                                                           locationTraitAttributeName,
                                                           1.0,
                                                           mostRecentSamplingDate);

        ConsoleProgressObserver progressObserver = new ConsoleProgressObserver();
        parser.registerProgressObserver(progressObserver);
        progressObserver.start ();

        String json = parser.parse();
        Gson gson = new Gson();
        SpreadData data = gson.fromJson(json, SpreadData.class);

        assertEquals("returns correct mrsd", mostRecentSamplingDate, data.getTimeline().getEndTime());
        assertEquals("returns correct root date", "2007/11/17", data.getTimeline().getStartTime());

        Attribute xCoordinatePointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(ParsersUtils.X_COORDINATE)).findAny().orElse(null);

        Attribute yCoordinatePointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(ParsersUtils.Y_COORDINATE)).findAny().orElse(null);

        assertArrayEquals("returns correct X coord range", new Double[]{108.1, 118.283}, xCoordinatePointAttribute.getRange());
        assertArrayEquals("returns correct Y coord range", new Double[]{22.3, 39.3583}, yCoordinatePointAttribute.getRange());

        Attribute countPointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(DiscreteTreeParser.COUNT)).findAny().orElse(null);

        assertArrayEquals("returns correct count attribute range", new Double[]{1.0, 16.0}, countPointAttribute.getRange());

        assertEquals("returns correct number of locations", 7, data.getLocations().size());

        Location loc = data.getLocations().get(0);
        assertEquals("returns correct id", "Fujian", loc.getId());
        assertEquals("returns correct x coordinate", 118.283, loc.getCoordinate().getXCoordinate(), 0.0);
        assertEquals("returns correct x coordinate", 25.917, loc.getCoordinate().getYCoordinate(), 0.0);

        // assertEquals("first layer is counts", Layer.Type.counts, data.getLayers().get(0).getType());
        assertEquals("returns correct number of Points in Layer", 20, data.getLayers().get(0).getPoints().size());

        // assertEquals("second layer is tree", Layer.Type.tree, data.getLayers().get(1).getType());
        assertEquals("returns correct number of points", 51, data.getLayers().get(1).getPoints().size());
        assertEquals("returns correct number of lines", 30, data.getLayers().get(1).getLines().size());
    }

}
