package com.spread.parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.GsonBuilder;
import com.spread.data.Attribute;
import com.spread.data.AxisAttributes;
import com.spread.data.Layer;
import com.spread.data.SpreadData;
import com.spread.data.Timeline;
import com.spread.data.attributable.Area;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;
import com.spread.data.primitive.Coordinate;
import com.spread.data.primitive.Polygon;
import com.spread.exceptions.SpreadException;
import com.spread.progress.IProgressObserver;
import com.spread.progress.IProgressReporter;
import com.spread.utils.ParsersUtils;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.trees.RootedTree;
import lombok.Setter;
import lombok.experimental.Accessors;

public class ContinuousTreeParser implements IProgressReporter {

    @Setter
    private String treeFilePath;
    @Setter
    private String xCoordinateAttributeName;
    @Setter
    private String yCoordinateAttributeName;
    @Setter
    private String hpdLevel;
    @Accessors(fluent = true)
    @Setter
    private boolean hasExternalAnnotations;
    @Setter
    private double timescaleMultiplier;
    @Setter
    private String mostRecentSamplingDate;
    private IProgressObserver progressObserver;

    public ContinuousTreeParser() {
    }

    public ContinuousTreeParser(String treeFilePath,
                                String xCoordinateAttributeName,
                                String yCoordinateAttributeName,
                                String hpdLevel,
                                boolean hasExternalAnnotations,
                                double timescaleMultiplier,
                                String mostRecentSamplingDate) {
        this.treeFilePath = treeFilePath;
        this.xCoordinateAttributeName = xCoordinateAttributeName;
        this.yCoordinateAttributeName = yCoordinateAttributeName;
        this.hpdLevel = hpdLevel;
        this.hasExternalAnnotations = hasExternalAnnotations;
        this.timescaleMultiplier = timescaleMultiplier;
        this.mostRecentSamplingDate = mostRecentSamplingDate;
    }

    protected SpreadData parseMccTree() throws IOException, ImportException, SpreadException {

        double progress = 0;
        double progressStepSize = 0;
        this.updateProgress(progress);

        RootedTree rootedTree = ParsersUtils.importRootedTree(treeFilePath);
        TimeParser timeParser = new TimeParser(this.mostRecentSamplingDate);
        Timeline timeline = timeParser.getTimeline(rootedTree.getHeight(rootedTree.getRootNode()));

        boolean externalAnnotations = this.hasExternalAnnotations;
        String hpd = this.hpdLevel;

        LinkedList<Line> linesList = new LinkedList<Line>();
        LinkedList<Point> pointsList = new LinkedList<Point>();
        LinkedList<Area> areasList = new LinkedList<Area>();
        LinkedList<Attribute> uniqueBranchAttributes = new LinkedList<Attribute>();
        LinkedList<Attribute> uniqueNodeAttributes = new LinkedList<Attribute>();
        LinkedList<Attribute> uniqueAreaAttributes = new LinkedList<Attribute>();

        HashMap<Node, Point> pointsMap = new HashMap<Node, Point>();

        // remove digits to get name
        String prefix = xCoordinateAttributeName.replaceAll("\\d*$", "");
        String modalityAttributeName = prefix.concat("_").concat(hpd).concat("%").concat("HPD_modality");

        // progress = 0;
        progressStepSize = 0.25 / (double) rootedTree.getNodes().size();
        for (Node node : rootedTree.getNodes()) {

            progress += progressStepSize;
            this.updateProgress(progress);

            if (!rootedTree.isRoot(node)) {

                // node parsed first
                Coordinate nodeCoordinate = null;
                Double nodeCoordinateX = null;
                Double nodeCoordinateY = null;
                int tryingCoordinate = 0;

                try {

                    tryingCoordinate = ParsersUtils.X_INDEX;
                    nodeCoordinateX = (Double) ParsersUtils.getObjectNodeAttribute(node, xCoordinateAttributeName);

                    tryingCoordinate = ParsersUtils.Y_INDEX;
                    nodeCoordinateY = (Double) ParsersUtils.getObjectNodeAttribute(node, yCoordinateAttributeName);

                } catch (SpreadException e) {
                    String coordinateName = (tryingCoordinate == ParsersUtils.X_INDEX ? xCoordinateAttributeName : yCoordinateAttributeName);
                    String nodeType = (rootedTree.isExternal(node) ? "external" : "internal");
                    String message = coordinateName + " attribute could not be found on the " + nodeType
                        + " child node. Resulting visualisation may be incomplete!";
                    System.out.println (message);
                    continue;
                }

                nodeCoordinate = new Coordinate(nodeCoordinateY, // latitude
                                                nodeCoordinateX // longitude
                                                );

                // ---POINTS PARSED FIRST DO NOT CHANGE ORDER---//

                Point nodePoint = pointsMap.get(node);
                if (nodePoint == null) {
                    nodePoint = createPoint(node, nodeCoordinate, rootedTree, timeParser);
                    pointsMap.put(node, nodePoint);
                }

                // parent node parsed second

                // this spills to the root node, resulting in exception if not anotated
                // root node will be annotated with locations but not with e.g. rate (facepalm)
                Node parentNode = rootedTree.getParent(node);

                Double parentCoordinateX = null;
                Double parentCoordinateY = null;
                tryingCoordinate = 0;

                try {
                    tryingCoordinate = ParsersUtils.X_INDEX;
                    parentCoordinateX = (Double) ParsersUtils.getObjectNodeAttribute(parentNode, xCoordinateAttributeName);

                    tryingCoordinate = ParsersUtils.Y_INDEX;
                    parentCoordinateY = (Double) ParsersUtils.getObjectNodeAttribute(parentNode, yCoordinateAttributeName);
                } catch (SpreadException e) {
                    String coordinateName = (tryingCoordinate == ParsersUtils.X_INDEX ? xCoordinateAttributeName : yCoordinateAttributeName);
                    String nodeType = (rootedTree.isExternal(parentNode) ? "external" : "internal");
                    String message = coordinateName + " attribute was found on the " + nodeType
                        + " child node but could not be found on the " + nodeType
                        + " parent node. Resulting visualisation may be incomplete!";
                    System.out.println (message);
                    continue;
                }

                Coordinate parentCoordinate = new Coordinate(parentCoordinateY, // lat
                                                             parentCoordinateX // long
                                                             );
                Point parentPoint = pointsMap.get(parentNode);
                if (parentPoint == null) {
                    parentPoint = createPoint(parentNode, parentCoordinate, rootedTree, timeParser);
                    pointsMap.put(parentNode, parentPoint);
                }

                // ---LINES PARSED SECOND DO NOT CHANGE ORDER---//

                Line line = new Line(parentPoint.getId(), //
                                     nodePoint.getId(), //
                                     parentPoint.getStartTime(), //
                                     nodePoint.getStartTime(), //
                                     nodePoint.getAttributes());

                linesList.add(line);

                // ---AREAS PARSED LAST DO NOT CHANGE ORDER---//

                boolean parseNode = true;
                if (rootedTree.isExternal(node)) {
                    parseNode = false;
                    if (externalAnnotations) {
                        parseNode = true;
                    }

                } else {
                    parseNode = true;
                }

                if (parseNode) {

                    Integer modality = 0;

                    try {

                        modality = (Integer) ParsersUtils.getObjectNodeAttribute(node, modalityAttributeName);

                    } catch (SpreadException e) {
                        // String nodeType = (rootedTree.isExternal(node) ? "external" : "internal");
                        // String message = modalityAttributeName + " attribute could not be found on the " + nodeType
                        //     + " node. Resulting visualisation may be incomplete!";
                        // System.out.println (message);
                        continue;
                    }

                    for (int m = 1; m <= modality; m++) {

                        // trait1_80%HPD_1
                        String xCoordinateHPDName = xCoordinateAttributeName.concat("_").concat(hpd).concat("%")
                            .concat(ParsersUtils.HPD.toUpperCase() + "_" + m);

                        String yCoordinateHPDName = yCoordinateAttributeName.concat("_").concat(hpd).concat("%")
                            .concat(ParsersUtils.HPD.toUpperCase() + "_" + m);

                        Object[] xCoordinateHPD = null;
                        Object[] yCoordinateHPD = null;
                        tryingCoordinate = 0;

                        try {

                            tryingCoordinate = ParsersUtils.X_INDEX;
                            xCoordinateHPD = ParsersUtils.getObjectArrayNodeAttribute(node, xCoordinateHPDName);

                            tryingCoordinate = ParsersUtils.Y_INDEX;
                            yCoordinateHPD = ParsersUtils.getObjectArrayNodeAttribute(node, yCoordinateHPDName);

                        } catch (SpreadException e) {
                            String coordinateName = (tryingCoordinate == ParsersUtils.X_INDEX ? xCoordinateHPDName
                                                     : yCoordinateHPDName);
                            String message = coordinateName
                                + " attribute could not be found on the child node. Resulting visualisation may be incomplete!";
                            System.out.println (message);
                            continue;
                        }

                        List<Coordinate> coordinateList = new ArrayList<Coordinate>();
                        for (int c = 0; c < xCoordinateHPD.length; c++) {
                            Double xCoordinate = (Double) xCoordinateHPD[c];
                            Double yCoordinate = (Double) yCoordinateHPD[c];

                            Coordinate coordinate = new Coordinate(yCoordinate, // lat
                                                                   xCoordinate // long
                                                                   );
                            coordinateList.add(coordinate);
                        }

                        Polygon polygon = new Polygon(coordinateList);

                        HashMap<String, Object> areaAttributesMap = new HashMap<String, Object>();
                        areaAttributesMap.putAll(nodePoint.getAttributes());
                        areaAttributesMap.put(ParsersUtils.HPD.toUpperCase(), hpd);

                        Area area = new Area(polygon, nodePoint.getStartTime(), areaAttributesMap);
                        areasList.add(area);

                    }

                }

            } else {

                Double rootCoordinateX = null;
                Double rootCoordinateY = null;
                int tryingCoordinate = 0;

                try {

                    tryingCoordinate = ParsersUtils.X_INDEX;
                    rootCoordinateX = (Double) ParsersUtils.getObjectNodeAttribute(node, xCoordinateAttributeName);

                    tryingCoordinate = ParsersUtils.Y_INDEX;
                    rootCoordinateY = (Double) ParsersUtils.getObjectNodeAttribute(node, yCoordinateAttributeName);

                } catch (SpreadException e) {
                    String coordinateName = (tryingCoordinate == ParsersUtils.X_INDEX ? xCoordinateAttributeName : yCoordinateAttributeName);
                    String message = coordinateName + " attribute was found on the root node."
                        + "Resulting visualisation may be incomplete!";
                    System.out.println (message);
                    continue;
                }

                Coordinate rootCoordinate = new Coordinate(rootCoordinateY, // lat
                                                           rootCoordinateX // long
                                                           );

                Point rootPoint = createPoint(node, rootCoordinate, rootedTree, timeParser);
                pointsMap.put(node, rootPoint);

            }
        }

        pointsList.addAll(pointsMap.values());

        // ---collect attributes from lines---//

        Map<String, Attribute> branchAttributesMap = new HashMap<String, Attribute>();

        progressStepSize = 0.25 / (double) linesList.size();
        for (Line line : linesList) {

            progress += progressStepSize;
            this.updateProgress(progress);

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

        // ---collect attributes from nodes---//

        Map<String, Attribute> nodeAttributesMap = new HashMap<String, Attribute>();

        progressStepSize = 0.25 / (double) pointsList.size();
        for (Point point : pointsList) {

            progress += progressStepSize;
            this.updateProgress(progress);

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

        uniqueNodeAttributes.addAll(nodeAttributesMap.values());

        // ---collect attributes from areas---//

        Map<String, Attribute> areasAttributesMap = new HashMap<String, Attribute>();

        progressStepSize = 0.24 / (double) areasList.size();
        for (Area area : areasList) {

            progress += progressStepSize;
            this.updateProgress(progress);

            for (Entry<String, Object> entry : area.getAttributes().entrySet()) {

                String attributeId = entry.getKey();
                Object attributeValue = entry.getValue();

                if (areasAttributesMap.containsKey(attributeId)) {

                    Attribute attribute = areasAttributesMap.get(attributeId);

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

                    areasAttributesMap.put(attributeId, attribute);

                } // END: key check

            } // END: attributes loop

        } // END: areas loop

        uniqueAreaAttributes.addAll(areasAttributesMap.values());

        AxisAttributes axis = new AxisAttributes(this.xCoordinateAttributeName,
                                                 this.yCoordinateAttributeName);

        LinkedList<Layer> layersList = new LinkedList<Layer>();

        // --- DATA LAYER (TREE LINES & POINTS, AREAS) --- //

        Layer treeLayer = new Layer.Builder ()
            .withPoints (pointsList)
            .withLines (linesList)
            .withAreas (areasList)
            .build ();

        layersList.add(treeLayer);

        this.updateProgress(0.90);

        return new SpreadData.Builder()
            .withAnalysisType(ParsersUtils.CONTINUOUS_TREE)
            .withTimeline(timeline)
            .withAxisAttributes(axis)
            .withLineAttributes(uniqueBranchAttributes)
            .withPointAttributes(uniqueNodeAttributes)
            .withAreaAttributes(uniqueAreaAttributes)
            .withLayers(layersList)
            .build();
    }

    public String parse() throws IOException, ImportException, SpreadException {
        SpreadData data = this.parseMccTree();
        this.updateProgress(1.0);
        return new GsonBuilder().create().toJson(data);
    }

    //  return attributes set and hpd levels
    public String parseAttributesAndHpdLevels() throws IOException, ImportException  {

        RootedTree tree = ParsersUtils.importRootedTree(this.treeFilePath);

        Set<String> uniqueAttributes = tree.getNodes().stream().filter(node -> !tree.isRoot(node))
            .flatMap(node -> node.getAttributeNames().stream())
            .map(name -> name)
            .collect(Collectors.toSet());

        Set<String> hpdLevels = uniqueAttributes.stream().filter(attributeName -> attributeName.contains("HPD_modality"))
            .map(hpdString -> hpdString.replaceAll("\\D+", ""))
            .collect(Collectors.toSet());

        Object pair = new Object[] {uniqueAttributes, hpdLevels};
        return new GsonBuilder().create().toJson(pair);
    }

    private Point createPoint(Node node, Coordinate coordinate, RootedTree rootedTree, TimeParser timeParser)
        throws SpreadException {

        Double height = ParsersUtils.getNodeHeight(rootedTree, node) * this.timescaleMultiplier;
        String startTime = timeParser.getNodeDate(height);

        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        for (String attributeName : node.getAttributeNames()) {

            Object nodeAttribute = node.getAttribute(attributeName);
            if (!(nodeAttribute instanceof Object[])) {
                // remove invalid characters
                attributeName = attributeName.replaceAll("%", "").replaceAll("!", "");
                attributes.put(attributeName, nodeAttribute);
            } // END: multivariate check

        } // END: attributes loop

        // annotate with node name
        Object value;
        if (rootedTree.isExternal(node)) {
            value = rootedTree.getTaxon(node).toString();
        } else if (rootedTree.isRoot(node)) {
            value = "root";
        } else {
            value = "internal";
        }

        attributes.put("nodeName", value);

        // external nodes have no posterior annotated, need to fix that
        if (rootedTree.isExternal(node)) {
            attributes.put(ParsersUtils.POSTERIOR, 1.0);
        }

        return new Point(coordinate, startTime, attributes);
    }

    @Override
    public void registerProgressObserver(IProgressObserver observer) {
        this.progressObserver = observer;
    }

    @Override
    public void updateProgress(double progress) {
        if (this.progressObserver != null) {
            this.progressObserver.handleProgress(progress);
        }
    }

}
