package com.spread;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class BayesFactorParserTest {

    @Test
    public void runTest() {

        String logFilename = "bayesFactor/H5N1_HA_discrete_rateMatrix.log";
        File logFile = new File(getClass().getClassLoader().getResource(logFilename).getFile());

        String locationsFilename = "bayesFactor/locationCoordinates_H5N1";
        File locationsFile = new File(getClass().getClassLoader().getResource(locationsFilename).getFile());



        // assertEquals("FUBAR", true, false);



    }

}
