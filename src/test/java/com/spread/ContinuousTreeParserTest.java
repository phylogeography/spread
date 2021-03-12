package com.spread;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import com.spread.data.Attribute;
import com.spread.data.SpreadData;
import com.spread.exceptions.SpreadException;
import com.spread.parsers.ContinuousTreeParser;

import org.junit.Test;

import jebl.evolution.io.ImportException;

public class ContinuousTreeParserTest {

    @Test
    public void runTest() throws IOException, ImportException, SpreadException {

        String filename = "continuous/speciesDiffusion.MCC.tre";
        File treefile = new File(getClass().getClassLoader().getResource(filename).getFile());
        String mostRecentSamplingDate = "2019/02/12";
        String xCoordinate = "trait2";
        String yCoordinate = "trait1";

        ContinuousTreeParser parser = new ContinuousTreeParser (treefile.getAbsolutePath(),
                                                                xCoordinate,
                                                                yCoordinate,
                                                                "80",
                                                                true,
                                                                1.0,
                                                                mostRecentSamplingDate);

        ConsoleProgressHandler progressHandler = new ConsoleProgressHandler(parser);
        progressHandler.start ();

        // parser.setProgressHandler (new ConsoleProgressHandler ());

        String json = parser.parse();
        Gson gson = new Gson();
        SpreadData data = gson.fromJson(json, SpreadData.class);

        assertEquals("returns correct mrsd", mostRecentSamplingDate, data.getTimeLine().getEndTime());
        assertEquals("returns correct root date", "2016/10/24", data.getTimeLine().getStartTime());

        assertEquals("returns correct X attribute", xCoordinate, data.getAxisAttributes().getXCoordinate());
        assertEquals("returns correct Y attribute", yCoordinate, data.getAxisAttributes().getYCoordinate());

        Attribute xCoordinateLineAttribute = data.getLineAttributes().stream().filter(att -> att.getId().equals(xCoordinate)).findAny().orElse(null);
        Attribute yCoordinateLineAttribute = data.getLineAttributes().stream().filter(att -> att.getId().equals(yCoordinate)).findAny().orElse(null);

        assertArrayEquals("returns correct X coord range", new Double[]{33.16, 176.0}, xCoordinateLineAttribute.getRange());
        assertArrayEquals("returns correct Y coord range", new Double[]{-39.0, -0.84}, yCoordinateLineAttribute.getRange());

        Attribute xCoordinatePointAttribute = data.getPointAttributes().stream().filter(att -> att.getId().equals(xCoordinate)).findAny().orElse(null);
        Attribute yCoordinatePointAttribute = data.getPointAttributes().stream().filter(att -> att.getId().equals(yCoordinate)).findAny().orElse(null);

        assertArrayEquals("returns correct X coord range", new Double[]{33.16, 176.0}, xCoordinatePointAttribute.getRange());
        assertArrayEquals("returns correct Y coord range", new Double[]{-39.0, -0.84}, yCoordinatePointAttribute.getRange());

        Attribute xCoordinateAreaAttribute = data.getAreaAttributes().stream().filter(att -> att.getId().equals(xCoordinate)).findAny().orElse(null);
        Attribute yCoordinateAreaAttribute = data.getAreaAttributes().stream().filter(att -> att.getId().equals(yCoordinate)).findAny().orElse(null);

        assertArrayEquals("returns correct X coord range", new Double[]{33.16, 142.71}, xCoordinateAreaAttribute.getRange());
        assertArrayEquals("returns correct Y coord range", new Double[]{-28.25, -0.84}, yCoordinateAreaAttribute.getRange());

        assertEquals("returns correct number of lines", 24, data.getLayers().get(0).getLines().size());
        assertEquals("returns correct number of points", 25, data.getLayers().get(0).getPoints().size());
        assertEquals("returns correct number of areas", 34, data.getLayers().get(0).getAreas().size());

    }

}
