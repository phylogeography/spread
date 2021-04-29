package com.spread;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.spread.data.Attribute;
import com.spread.data.SpreadData;
import com.spread.exceptions.SpreadException;
import com.spread.parsers.BayesFactorParser;
import com.spread.parsers.BayesFactorParser.BayesFactor;
import com.spread.utils.ParsersUtils;

import org.junit.Test;

public class BayesFactorParserTest {

    @Test
    public void runTest() throws IOException, SpreadException {

        String logFilename = "bayesFactor/H5N1_HA_discrete_rateMatrix.log";
        File logFile = new File(getClass().getClassLoader().getResource(logFilename).getFile());

        String locationsFilename = "bayesFactor/locationCoordinates_H5N1";
        File locationsFile = new File(getClass().getClassLoader().getResource(locationsFilename).getFile());

        BayesFactorParser parser = new BayesFactorParser(logFile.getAbsolutePath(),
                                                         0.1,
                                                         locationsFile.getAbsolutePath());

        ConsoleProgressObserver progressObserver = new ConsoleProgressObserver();
        parser.registerProgressObserver(progressObserver);
        progressObserver.start ();

        LinkedList<BayesFactor> expected =
            new LinkedList<BayesFactor>(Arrays.asList(new BayesFactor ("Fujian", "Guangdong", 19.014687619229807, 0.8989450305385897),
                                                      new BayesFactor ("Fujian", "Guangxi", 2.914568793008483, 0.5769017212659634),
                                                      new BayesFactor ("Fujian", "Hebei", 5.381417549046571, 0.7157134925041644),
                                                      new BayesFactor ("Fujian", "Henan", 2.8620793773994855, 0.572459744586341),
                                                      new BayesFactor ("Fujian", "HongKong", 0.43579473603211477, 0.1693503609106052),
                                                      new BayesFactor ("Fujian", "Hunan", 0.7591555160396912, 0.2620766240977235),
                                                      new BayesFactor ("Guangdong", "Guangxi", 5.751202221591034, 0.7290394225430317),
                                                      new BayesFactor ("Guangdong", "Hebei", 0.5046759684413089, 0.19100499722376457),
                                                      new BayesFactor ("Guangdong", "Henan", 0.49024749921100813, 0.18656302054414214),
                                                      new BayesFactor ("Guangdong", "HongKong", 0.7289591633549846, 0.2543031649083842),
                                                      new BayesFactor ("Guangdong", "Hunan", 0.3020727841804354, 0.12382009994447529),
                                                      new BayesFactor ("Guangxi", "Hebei", 0.5811807357716423, 0.21377012770682954),
                                                      new BayesFactor ("Guangxi", "Henan", 0.5433155798506233, 0.20266518600777345),
                                                      new BayesFactor ("Guangxi", "HongKong", 3.599727763150519, 0.6274292059966685),
                                                      new BayesFactor ("Guangxi", "Hunan", 1.3275435166126814, 0.38312048861743475),
                                                      new BayesFactor ("Hebei", "Henan", 1.7787442947654648, 0.45419211549139366),
                                                      new BayesFactor ("Hebei", "HongKong", 0.08643998969094537, 0.03886729594669628),
                                                      new BayesFactor ("Hebei", "Hunan", 0.9897614555920281, 0.3164908384230983),
                                                      new BayesFactor ("Henan", "HongKong", 0.05602662286635096, 0.025541365907828985),
                                                      new BayesFactor ("Henan", "Hunan", 4.6163308990686085, 0.6835091615769017),
                                                      new BayesFactor ("HongKong", "Hunan", 0.06103762019867277, 0.0277623542476402)));

        String json = parser.parse();
        Gson gson = new Gson();
        BayesFactorParser.BayesFactorParserOutput output = gson.fromJson(json, BayesFactorParser.BayesFactorParserOutput.class);
        SpreadData data = output.spreadData;

        assertEquals("returns correct type", ParsersUtils.BAYES_FACTOR, data.getAnalysisType());

        List<BayesFactor> bayesFactors = output.bayesFactors;
        assertEquals("Bayes Factors", expected, bayesFactors);

        assertEquals("xCoordinate", data.getAxisAttributes().getXCoordinate());
        assertEquals("yCoordinate", data.getAxisAttributes().getYCoordinate());

        Set<String> locationIds = data.getLocations().stream().map(location -> location.getId()).collect(Collectors.toSet());
        Set<String> expectedLocationIds = new HashSet<>(Arrays.asList("Fujian", "Guangxi", "Guangdong", "Hebei", "Hunan", "Henan", "HongKong"));
        assertEquals("all locations present", expectedLocationIds, locationIds);

        Attribute bayesFactorAttribute = data.getLineAttributes().stream()
            .filter(att -> att.getId().equals(BayesFactorParser.BAYES_FACTOR)).findAny().orElse(null);
        assertArrayEquals("returns correct bayesFactor attribute range", new Double[]{0.06, 19.014687619229807}, bayesFactorAttribute.getRange());

        Attribute posteriorProbabilityAttribute = data.getLineAttributes().stream()
            .filter(att -> att.getId().equals(BayesFactorParser.POSTERIOR_PROBABILITY)).findAny().orElse(null);
        assertArrayEquals("returns correct posteriorProbability attribute range", new Double[]{0.03, 0.8989450305385897}, posteriorProbabilityAttribute.getRange());

        Attribute xCoordinatePointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(ParsersUtils.X_COORDINATE)).findAny().orElse(null);

        assertArrayEquals("returns correct X coord range", new Double[]{108.1, 118.283}, xCoordinatePointAttribute.getRange());

        Attribute yCoordinatePointAttribute =
            data.getPointAttributes().stream().filter(att -> att.getId().equals(ParsersUtils.Y_COORDINATE)).findAny().orElse(null);

        assertArrayEquals("returns correct Y coord range", new Double[]{22.3, 39.3583}, yCoordinatePointAttribute.getRange());

    }

}
