package com.spread;

import org.junit.Test;

import jebl.evolution.io.ImportException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import com.spread.exceptions.SpreadException;

import java.io.File;
import java.io.IOException;

import com.spread.parsers.TimeSlicerParser;

public class TimeSlicerParserTest {

    @Test
    public void runTest() throws Exception
    {

        String filename = "timeSlicer/WNV_small.trees";
        // String filename = "/home/filip/Dropbox/JavaProjects/SpreaD3/data/continuous/WNV/WNV_relaxed_geo_gamma.trees";
        File treesfile = new File(getClass().getClassLoader().getResource(filename).getFile());

        TimeSlicerParser parser = new TimeSlicerParser (treesfile.getAbsolutePath(),
                                                        1,
                                                        10,
                                                        "location",
                                                        "rate"
                                                        );

        String json = parser.parse();

    }

}
