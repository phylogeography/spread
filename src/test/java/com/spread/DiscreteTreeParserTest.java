package com.spread;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import com.spread.data.Attribute;
import com.spread.data.SpreadData;
import com.spread.exceptions.SpreadException;
import com.spread.parsers.DiscreteTreeParser;

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
                                                           mostRecentSamplingDate
                                                           );

        String json = parser.parse();
        Gson gson = new Gson();
        SpreadData data = gson.fromJson(json, SpreadData.class);

        assertEquals("returns correct mrsd", mostRecentSamplingDate, data.getTimeLine().getEndTime());
        assertEquals("returns correct root date", "2007/11/17", data.getTimeLine().getStartTime());

        Attribute xCoordinatePointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(DiscreteTreeParser.X_COORDINATE)).findAny().orElse(null);

        Attribute yCoordinatePointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(DiscreteTreeParser.Y_COORDINATE)).findAny().orElse(null);

        assertArrayEquals("returns correct X coord range", new Double[]{108.1, 118.283}, xCoordinatePointAttribute.getRange());
        assertArrayEquals("returns correct Y coord range", new Double[]{22.3, 39.3583}, yCoordinatePointAttribute.getRange());

        Attribute countPointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(DiscreteTreeParser.COUNT)).findAny().orElse(null);

        assertArrayEquals("returns correct count attribute range", new Double[]{1.0, 16.0}, countPointAttribute.getRange());

        // Fujian	25.917	118.283

        System.out.println ("@@@" + data.getLocations().size());



        // assertEquals("returns correct number of lines", 24, data.getLayers().get(0).getLines().size());
        // assertEquals("returns correct number of points", 25, data.getLayers().get(0).getPoints().size());
        // assertEquals("returns correct number of areas", 34, data.getLayers().get(0).getAreas().size());

    }

}
