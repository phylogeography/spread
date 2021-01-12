package com.spread;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import com.spread.parsers.TimeSlicerParser;

public class TimeSlicerParserTest {

    @Test
    public void runTest() throws IOException {

        String filename = "timeSlicer/WNV_small.trees";
        File treesfile = new File(getClass().getClassLoader().getResource(filename).getFile());

        TimeSlicerParser parser = new TimeSlicerParser (treesfile.getAbsolutePath(),
                                                        "todo");

        parser.parse();

    }


}
