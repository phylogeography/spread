package com.spread.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.spread.exceptions.SpreadException;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.io.TreeImporter;
import jebl.evolution.trees.RootedTree;

public class ParsersUtils {

    public static final double EARTH_RADIUS = 6371.0;

    public static final String EMPTY_STRING = "";
    public static final String BLANK_SPACE = "\\s+";
    public static final String TAB = "\t";
    public static final String HASH_COMMENT = "#";
    public static final String INDICATORS = "indicators";
    public static final String DURATION = "duration";
    public static final String DISTANCE = "distance";
    public static final String LOCATION = "location";
    public static final String POSTERIOR = "posterior";
    // public static final String HPD = "hpd";
    public static final String START = "start";
    public static final String END = "end";
    public static final String ONE = "1";
    public static final String TWO = "2";
    public static final String RATE = "rate";
    public static final String PRECISION = "precision";
    public static final int LATITUDE_INDEX = 0;
    public static final int LONGITUDE_INDEX = 1;
    public static final String HPD = "hpd";
    public static final String TRAIT = "trait";
    public static final int YEAR_INDEX = 0;
    public static final int MONTH_INDEX = 1;
    public static final int DAY_INDEX = 2;
    public static final int X_INDEX = 0;
    public static final int Y_INDEX = 1;
    public static final String NEGATIVE_SIGN = "-";

    // Use this for all random numbers
    private static final Random random = new Random();

    public static String[] readLines(String filename, String comment) throws IOException {

        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();

        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            // skip commented lines
            if (!line.contains(comment)) {
                lines.add(line);
            }
        }

        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
    }

    public static String splitString(String string, String c) {
        String[] id = string.split(c);
        return id[id.length - 1];
    }

    public static RootedTree importRootedTree(String treefile) throws IOException, ImportException {
        TreeImporter importer = new NexusImporter(new FileReader(treefile));
        return (RootedTree) importer.importNextTree();
    }

    public static Double getNodeHeight(RootedTree tree, Node node) throws SpreadException {
        return tree.getHeight(node);
    }

    public static Object getObjectNodeAttribute(Node node, String attributeName) throws SpreadException {
        Object nodeAttribute = node.getAttribute(attributeName);
        if (nodeAttribute == null) {
            throw new SpreadException("Attribute " + attributeName + " missing from the node. \n");
        }
        return nodeAttribute;
    }

    public static Object[] getObjectArrayNodeAttribute(Node node, String attributeName) throws SpreadException {
        Object[] nodeAttributeArray = (Object[]) node.getAttribute(attributeName);
        if (nodeAttributeArray == null) {
            throw new SpreadException("Attribute " + attributeName + " missing from the node. \n");
        }
        return nodeAttributeArray;
    }

    public static double round(double value, double precision) {
        return (double) Math.round(value * precision) / precision;
    }

    public static Object pickRand(Object[] array) {
        int rnd = random.nextInt(array.length);
        return array[rnd];
    }

    public static String breakTiesRandomly(String tiedState) {

        String[] array = tiedState.split("\\+");
        String state = (String) pickRand(array);

        return state;
    }

}
