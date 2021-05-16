package com.spread;

import org.junit.Test;

import jebl.evolution.io.ImportException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.spread.data.SpreadData;
import com.spread.exceptions.SpreadException;
import com.spread.data.Attribute;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.spread.parsers.TimeSlicerParser;
import com.spread.utils.ParsersUtils;

public class TimeSlicerParserTest {

    @Test
    public void runTest() throws IOException, ImportException, SpreadException {

        Gson gson = new Gson();

        String mostRecentSamplingDate = "2021/01/12";
        double hpdLevel = 0.8;
        String path = "timeSlicer/WNV_small.trees";
        File treesfile = new File(getClass().getClassLoader().getResource(path).getFile());

        TimeSlicerParser parser = new TimeSlicerParser (treesfile.getAbsolutePath(),
                                                        0.1,
                                                        10,
                                                        "location",
                                                        "rate", // pass null for a rate of 1
                                                        hpdLevel,
                                                        100,
                                                        mostRecentSamplingDate,
                                                        1.0
                                                        );

        Set<String> expectedAttributes = new HashSet<>(Arrays.asList("rate", "location"));
        Set<String> uniqueAttributes = gson.fromJson(parser.parseAttributes(), new TypeToken<Set<String>>(){}.getType());

        assertEquals("attributes are parsed", expectedAttributes, uniqueAttributes);

        ConsoleProgressObserver progressObserver = new ConsoleProgressObserver();
        parser.registerProgressObserver(progressObserver);
        progressObserver.start ();

        String json = parser.parse();
        SpreadData data = gson.fromJson(json, SpreadData.class);

        assertEquals("returns correct type", ParsersUtils.TIME_SLICER, data.getAnalysisType());

        assertEquals("returns correct mrsd", mostRecentSamplingDate, data.getTimeline().getEndTime());
        assertEquals("returns correct root date", "2011/04/28", data.getTimeline().getStartTime());

        Attribute hpdAreaAttribute = data.getAreaAttributes().stream().filter(att -> att.getId().equals(ParsersUtils.HPD.toUpperCase())).findAny().orElse(null);

        assertArrayEquals("returns correct HPD attribute range", new Double[]{hpdLevel, hpdLevel}, hpdAreaAttribute.getRange());
        assertTrue("Areas are generated", data.getAreas().size() > 0);

        // assertEquals("returns correct number of points", 207, data.getPoints().size());
        // assertEquals("returns correct number of lines", 206, data.getLines().size());

    }
}
