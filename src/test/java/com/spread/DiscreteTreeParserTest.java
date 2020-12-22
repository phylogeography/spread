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
        // Gson gson = new Gson();
        // SpreadData data = gson.fromJson(json, SpreadData.class);




    }

}
