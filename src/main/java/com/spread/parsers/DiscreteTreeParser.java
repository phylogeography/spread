package com.spread.parsers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.spread.data.primitive.Coordinate;
import com.google.gson.GsonBuilder;
import com.spread.data.AxisAttributes;
import com.spread.data.Attribute;
import com.spread.data.Layer;
import com.spread.data.Location;
import com.spread.data.TimeLine;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;
import com.spread.exceptions.SpreadException;
import com.spread.utils.ParsersUtils;
import com.spread.data.SpreadData;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.trees.RootedTree;
import lombok.Getter;
import lombok.Setter;

public class DiscreteTreeParser {

    public static final String X_COORDINATE = "xCoordinate";
    public static final String Y_COORDINATE = "yCoordinate";
    public static final String COUNT = "count";
    private static final Integer UNRESOLVED_INDEX = Integer.MAX_VALUE;

    @Getter @Setter
    private String treeFilePath;
    @Getter @Setter
    private String locationsFilePath;
    @Getter @Setter
    private String locationTraitAttributeName;
    @Getter @Setter
    private double timescaleMultiplier;
    @Getter @Setter
    private String mostRecentSamplingDate;

    public DiscreteTreeParser() {
    }

    public DiscreteTreeParser(String treeFilePath,
                              String locationsFilePath,
                              String locationTraitAttributeName,
                              double timescaleMultiplier,
                              String mostRecentSamplingDate
                              ) {
        this.treeFilePath = treeFilePath;
        this.locationsFilePath = locationsFilePath;
        this.locationTraitAttributeName = locationTraitAttributeName;
        this.timescaleMultiplier = timescaleMultiplier;
        this.mostRecentSamplingDate = mostRecentSamplingDate;
    }// END: Constructor

    public String parse() throws IOException, ImportException, SpreadException {

        RootedTree rootedTree = ParsersUtils.importRootedTree(this.treeFilePath);
        TimeParser timeParser = new TimeParser(this.mostRecentSamplingDate);
        TimeLine timeLine = timeParser.getTimeLine(rootedTree.getHeight(rootedTree.getRootNode()));

        DiscreteLocationsParser locationsParser = new DiscreteLocationsParser(this.locationsFilePath, false);
        LinkedList<Location> locationsList = locationsParser.parseLocations();

        LinkedList<Attribute> uniqueBranchAttributes = new LinkedList<Attribute>();
        LinkedList<Attribute> uniqueNodeAttributes = new LinkedList<Attribute>();

        LinkedList<Line> linesList = new LinkedList<Line>();
        LinkedList<Point> pointsList = new LinkedList<Point>();
        LinkedList<Point> countsList = new LinkedList<Point>();

        HashMap<Node, Point> pointsMap = new HashMap<Node, Point>();

        double rootHeight = rootedTree.getHeight(rootedTree.getRootNode());
        Double[] sliceHeights = createSliceHeights(rootHeight, 10);
        int[][] locationCounts = new int[sliceHeights.length][locationsList.size()];

        Location dummy;
        for (Node node : rootedTree.getNodes()) {
            if (!rootedTree.isRoot(node)) {

                // node parsed first
                String nodeState = getNodeState(node, this.locationTraitAttributeName);

                dummy = new Location(nodeState);
                int locationIndex = UNRESOLVED_INDEX;
                if (locationsList.contains(dummy)) {
                    locationIndex = locationsList.indexOf(dummy);
                } else {
                    String message1 = "Location " + dummy.getId() + " could not be found in the locations file.";
                    String message2 = "Resulting file may be incomplete!";
                    System.out.println(message1 + " " + message2);
                    continue;
                }

                Location nodeLocation = locationsList.get(locationIndex);

                // parent node parsed second
                Node parentNode = rootedTree.getParent(node);
                String parentState = getNodeState(parentNode, this.locationTraitAttributeName);

                dummy = new Location(parentState);
                locationIndex = UNRESOLVED_INDEX;
                if (locationsList.contains(dummy)) {

                    locationIndex = locationsList.indexOf(dummy);

                } else {

                    String message = "Parent location " + dummy.getId() + " could not be found in the locations file.";
                    throw new SpreadException(message);
                }

                Location parentLocation = locationsList.get(locationIndex);

                if (!parentLocation.equals(nodeLocation)) {

                    Point parentPoint = pointsMap.get(parentNode);
                    if (parentPoint == null) {

                        parentPoint = createPoint(parentNode, parentLocation, rootedTree, timeParser);
                        pointsMap.put(parentNode, parentPoint);

                    } // END: null check

                    Point nodePoint = pointsMap.get(node);
                    if (nodePoint == null) {

                        nodePoint = createPoint(node, nodeLocation, rootedTree, timeParser);
                        pointsMap.put(node, nodePoint);

                    } // END: null check

                    Line line = new Line(parentPoint.getId(), //
                                         nodePoint.getId(), //
                                         parentPoint.getStartTime(), //
                                         nodePoint.getStartTime(), //
                                         nodePoint.getAttributes() //
                                         );

                    linesList.add(line);

                } else {

                    // count lineages holding state
                    for (int i = 0; i < sliceHeights.length; i++) {

                        double sliceHeight = sliceHeights[i];
                        for (Location location : locationsList) {

                            if ((rootedTree.getHeight(node) <= sliceHeight)
                                && (rootedTree.getHeight(parentNode) > sliceHeight)) {

                                if (nodeLocation.equals(parentLocation) && parentLocation.equals(location)) {

                                    int j = locationsList.lastIndexOf(location);
                                    locationCounts[i][j]++;

                                } // END: location check

                            } // END:
                        } // END: locations loop
                    } // END: sliceHeights lop

                } // END: state check

            } else {

                System.out.println("At the root node");

                String rootState = getNodeState(node, this.locationTraitAttributeName);

                dummy = new Location(rootState);
                int locationIndex = UNRESOLVED_INDEX;
                if (locationsList.contains(dummy)) {
                    locationIndex = locationsList.indexOf(dummy);
                } else {

                    String message1 = "Location " + dummy.getId() + " of the root node could not be found in the locations file.";
                    String message2 = "Resulting file may be incomplete!";
                    System.out.println(message1 + " " + message2);
                    continue;

                }

                Location location = locationsList.get(locationIndex);
                Point rootPoint = createPoint(node, location, rootedTree, timeParser);
                pointsMap.put(node, rootPoint);

            } // END: root check
        } // END: node loop

        pointsList.addAll(pointsMap.values());

        // create Points list with count attributes

        Double[] countRange = new Double[2];
        countRange[Attribute.MIN_INDEX] = Double.MAX_VALUE;
        countRange[Attribute.MAX_INDEX] = Double.MIN_VALUE;

        for (int sliceIndex = 0; sliceIndex < locationCounts.length; sliceIndex++) {

            double height = sliceHeights[sliceIndex];
            double nextHeight = sliceIndex < locationCounts.length - 1 ? sliceHeights[sliceIndex + 1] : 0.0;

            for (int locationIndex = 0; locationIndex < locationCounts[0].length; locationIndex++) {

                Double count = (double) locationCounts[sliceIndex][locationIndex];
                if (count > 0) {

                    Location location = locationsList.get(locationIndex);
                    String startTime = timeParser.getNodeDate(height);
                    String endTime = timeParser.getNodeDate(nextHeight);

                    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
                    attributes.put(COUNT, locationCounts[sliceIndex][locationIndex]);

                    Point point = new Point(location.getId(), startTime, endTime, attributes);
                    countsList.add(point);

                    if (count < countRange[Attribute.MIN_INDEX]) {
                        countRange[Attribute.MIN_INDEX] = count;
                    } // END: min check

                    if (count > countRange[Attribute.MAX_INDEX]) {
                        countRange[Attribute.MAX_INDEX] = count;
                    } // END: max check

                }

            } // END: locations loop
        } // END: slice loop

        Attribute countAttribute = new Attribute(COUNT, countRange);

        // collect attributes from lines
        Map<String, Attribute> branchAttributesMap = new HashMap<String, Attribute>();

        for (Line line : linesList) {

            for (Entry<String, Object> entry : line.getAttributes().entrySet()) {

                String attributeId = entry.getKey();
                Object attributeValue = entry.getValue();

                if (branchAttributesMap.containsKey(attributeId)) {

                    Attribute attribute = branchAttributesMap.get(attributeId);

                    if (attribute.getScale().equals(Attribute.ORDINAL)) {

                        attribute.getDomain().add(attributeValue);

                    } else {

                        double value = ParsersUtils.round(Double.valueOf(attributeValue.toString()), 100);

                        if (value < attribute.getRange()[Attribute.MIN_INDEX]) {
                            attribute.getRange()[Attribute.MIN_INDEX] = value;
                        } // END: min check

                        if (value > attribute.getRange()[Attribute.MAX_INDEX]) {
                            attribute.getRange()[Attribute.MAX_INDEX] = value;
                        } // END: max check

                    } // END: scale check

                } else {

                    Attribute attribute;
                    if (attributeValue instanceof Double) {

                        Double[] range = new Double[2];
                        range[Attribute.MIN_INDEX] = (Double) attributeValue;
                        range[Attribute.MAX_INDEX] = (Double) attributeValue;

                        attribute = new Attribute(attributeId, range);

                    } else {

                        HashSet<Object> domain = new HashSet<Object>();
                        domain.add(attributeValue);

                        attribute = new Attribute(attributeId, domain);

                    } // END: isNumeric check

                    branchAttributesMap.put(attributeId, attribute);

                } // END: key check

            } // END: attributes loop

        } // END: lines loop

        uniqueBranchAttributes.addAll(branchAttributesMap.values());

        // collect attributes from nodes
        Map<String, Attribute> nodeAttributesMap = new HashMap<String, Attribute>();

        for (Point point : pointsList) {

            for (Entry<String, Object> entry : point.getAttributes().entrySet()) {

                String attributeId = entry.getKey();
                Object attributeValue = entry.getValue();

                if (nodeAttributesMap.containsKey(attributeId)) {

                    Attribute attribute = nodeAttributesMap.get(attributeId);

                    if (attribute.getScale().equals(Attribute.ORDINAL)) {

                        attribute.getDomain().add(attributeValue);

                    } else {

                        double value = ParsersUtils.round(Double.valueOf(attributeValue.toString()), 100);

                        if (value < attribute.getRange()[Attribute.MIN_INDEX]) {
                            attribute.getRange()[Attribute.MIN_INDEX] = value;
                        } // END: min check

                        if (value > attribute.getRange()[Attribute.MAX_INDEX]) {
                            attribute.getRange()[Attribute.MAX_INDEX] = value;
                        } // END: max check

                    } // END: scale check

                } else {

                    Attribute attribute;
                    if (attributeValue instanceof Double) {

                        Double[] range = new Double[2];
                        range[Attribute.MIN_INDEX] = (Double) attributeValue;
                        range[Attribute.MAX_INDEX] = (Double) attributeValue;

                        attribute = new Attribute(attributeId, range);

                    } else {

                        HashSet<Object> domain = new HashSet<Object>();
                        domain.add(attributeValue);

                        attribute = new Attribute(attributeId, domain);

                    } // END: isNumeric check

                    nodeAttributesMap.put(attributeId, attribute);

                } // END: key check

            } // END: attributes loop

        } // END: points loop

        uniqueNodeAttributes.addAll(branchAttributesMap.values());
        // we dump it here with node attributes
        uniqueNodeAttributes.add(countAttribute);

        // --- LAYERS --- //

        LinkedList<Layer> layersList = new LinkedList<Layer>();

        String countsLayerId = ParsersUtils.splitString(this.treeFilePath , "/");
        Layer countsLayer = new Layer(countsLayerId, //
                                      "Counts layer", //
                                      countsList //
                                      );

        layersList.add(countsLayer);

        String treeLayerId = ParsersUtils.splitString(this.treeFilePath , "/");
        Layer treeLayer = new Layer(treeLayerId, //
                                    "Tree layer", //
                                    pointsList, //
                                    linesList //
                                    );

        layersList.add(treeLayer);

        LinkedList<Attribute> rangeAttributes = getCoordinateRangeAttributes(locationsList);
        Attribute xCoordinate = rangeAttributes.get(ParsersUtils.X_INDEX);
        Attribute yCoordinate = rangeAttributes.get(ParsersUtils.Y_INDEX);

        uniqueNodeAttributes.add(xCoordinate);
        uniqueNodeAttributes.add(yCoordinate);
        AxisAttributes axis = new AxisAttributes(xCoordinate.getId(),
                                                 yCoordinate.getId());

        SpreadData spreadData= new SpreadData(timeLine, //
                                              axis, //
                                              // mapAttributes, //
                                              uniqueBranchAttributes, //
                                              uniqueNodeAttributes, //
                                              null, // areaAttributes
                                              locationsList, //
                                              layersList //
                                              );

        return new GsonBuilder().create().toJson(spreadData);
    }// END: parse

    private LinkedList<Attribute> getCoordinateRangeAttributes(LinkedList<Location> locationsList) throws SpreadException {

        LinkedList<Attribute> coordinateRange = new LinkedList<Attribute>();

        Double[] xCoordinateRange = new Double[2];
        xCoordinateRange[Attribute.MIN_INDEX] = Double.MAX_VALUE;
        xCoordinateRange[Attribute.MAX_INDEX] = Double.MIN_VALUE;

        Double[] yCoordinateRange = new Double[2];
        yCoordinateRange[Attribute.MIN_INDEX] = Double.MAX_VALUE;
        yCoordinateRange[Attribute.MAX_INDEX] = Double.MIN_VALUE;

        for (Location location : locationsList) {

            Coordinate coordinate = location.getCoordinate();
            if (coordinate == null) {
                throw new SpreadException("Location " + location.getId()
                                          + " has no coordinates set.");
            }

            Double latitude = coordinate.getYCoordinate();
            Double longitude = coordinate.getXCoordinate();

            // update coordinates range

            if (latitude < yCoordinateRange[Attribute.MIN_INDEX]) {
                yCoordinateRange[Attribute.MIN_INDEX] = latitude;
            } // END: min check

            if (latitude > yCoordinateRange[Attribute.MAX_INDEX]) {
                yCoordinateRange[Attribute.MAX_INDEX] = latitude;
            } // END: max check

            if (longitude < xCoordinateRange[Attribute.MIN_INDEX]) {
                xCoordinateRange[Attribute.MIN_INDEX] = longitude;
            } // END: min check

            if (longitude > xCoordinateRange[Attribute.MAX_INDEX]) {
                xCoordinateRange[Attribute.MAX_INDEX] = longitude;
            } // END: max check

        }

        Attribute xCoordinate = new Attribute(X_COORDINATE, xCoordinateRange);
        Attribute yCoordinate = new Attribute(Y_COORDINATE, yCoordinateRange);

        coordinateRange.add(ParsersUtils.X_INDEX, xCoordinate);
        coordinateRange.add(ParsersUtils.Y_INDEX, yCoordinate);

        return coordinateRange;
    }// END: getCoordinateRange

    private Double[] createSliceHeights(double rootHeight, int intervals) {
        // double rootHeight = rootedTree.getHeight(rootedTree.getRootNode());
        double delta = rootHeight / (double) intervals;

        Double[] sliceHeights = new Double[intervals - 1];
        for (int i = 0; i < (intervals - 1); i++) {
            sliceHeights[i] = rootHeight - ((i + 1) * delta);
        }

        return sliceHeights;
    }// END: createSliceHeights

    private String getNodeState(Node node, String locationTraitAttributeName) throws SpreadException {

        String nodeState = (String) ParsersUtils.getObjectNodeAttribute(node, locationTraitAttributeName);
        if (nodeState.contains("+")) {
            String message = "Found tied state " + nodeState;
            nodeState = ParsersUtils.breakTiesRandomly(nodeState);
            message += (" randomly choosing " + nodeState);
            System.out.println(message);
        } // END: tie check

        return nodeState;
    }

    private Point createPoint(Node node, Location location, RootedTree rootedTree, TimeParser timeParser) throws SpreadException {

        Double height = ParsersUtils.getNodeHeight(rootedTree, node) * timescaleMultiplier;
        String startTime = timeParser.getNodeDate(height);

        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        for (String attributeName : node.getAttributeNames()) {

            Object nodeAttribute = node.getAttribute(attributeName);

            if (!(nodeAttribute instanceof Object[])) {

                // remove invalid characters
                attributeName = attributeName.replaceAll("%", "");
                attributeName = attributeName.replaceAll("!", "");

                attributes.put(attributeName, nodeAttribute);

            } // END: multivariate check

        } // END: attributes loop

        // annotate with node name
        Object value;
        if (rootedTree.isExternal(node)) {
            value = rootedTree.getTaxon(node).toString();
        } else if(rootedTree.isRoot(node)) {
            value = "root";
        } else {
            value = "internal";
        }

        String attributeName = "nodeName";
        attributes.put(attributeName, value);

        // external nodes have no posterior annotated, so fix that
        if (rootedTree.isExternal(node)) {
            attributes.put(ParsersUtils.POSTERIOR, 1.0);
        }

        Point point = new Point(location.getId(), startTime, attributes);

        return point;
    }// END: createPoint

}// END: class
