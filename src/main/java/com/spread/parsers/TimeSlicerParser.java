package com.spread.parsers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gson.GsonBuilder;
import com.spread.contouring.ContourMaker;
import com.spread.contouring.ContourPath;
import com.spread.contouring.ContourWithSnyder;
import com.spread.data.Attribute;
import com.spread.data.Layer;
import com.spread.data.SpreadData;
import com.spread.data.Timeline;
import com.spread.data.attributable.Area;
import com.spread.data.primitive.Coordinate;
import com.spread.data.primitive.Polygon;
import com.spread.exceptions.SpreadException;
import com.spread.progress.IProgressObserver;
import com.spread.progress.IProgressReporter;
import com.spread.utils.ParsersUtils;

import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.RootedTree;
import lombok.Setter;

public class TimeSlicerParser implements IProgressReporter {

    @Setter
    private String treesFilePath;
    @Setter
    private String sliceHeightsFilePath;
    @Setter
    private int numberOfIntervals;
    @Setter
    private double burnIn;
    @Setter
    private String traitName;
    @Setter
    private String rrwRateName;
    @Setter
    private double hpdLevel;
    @Setter
    private int gridSize;
    @Setter
    private String mostRecentSamplingDate;
    @Setter
    private double timescaleMultiplier;
    // @Setter
    // private String mccTreeFilePath;
    // @Setter
    // private String xCoordinateAttributeName;
    // @Setter
    // private String yCoordinateAttributeName;
    private IProgressObserver progressObserver;

    public TimeSlicerParser() {
    }

    public TimeSlicerParser(String treesFilePath, // path to the trees file
                            double burnIn, // ignore first n trees
                            String sliceHeightsFilePath,
                            String traitName, // 2D trait for contouring
                            String rrwRateName, // relaxed random walk rate attribute name
                            double hpdLevel,
                            int gridSize,
                            String mostRecentSamplingDate,
                            double timescaleMultiplier
                            // String mccTreeFilePath,
                            // String xCoordinateAttributeName,
                            // String yCoordinateAttributeName
                            ) {
        this.treesFilePath = treesFilePath;
        this.sliceHeightsFilePath = sliceHeightsFilePath;
        this.burnIn = burnIn;
        this.traitName = traitName;
        this.rrwRateName = rrwRateName;
        this.hpdLevel = hpdLevel;
        this.gridSize = gridSize;
        this.mostRecentSamplingDate = mostRecentSamplingDate;
        this.timescaleMultiplier = timescaleMultiplier;
        // this.mccTreeFilePath = mccTreeFilePath;
        // this.xCoordinateAttributeName = xCoordinateAttributeName;
        // this.yCoordinateAttributeName = yCoordinateAttributeName;
    }

    public TimeSlicerParser(String treesFilePath,
                            double burnIn,
                            int numberOfIntervals,
                            String traitName,
                            String rrwRateName,
                            double hpdLevel,
                            int gridSize,
                            String mostRecentSamplingDate,
                            double timescaleMultiplier,
                            String mccTreeFilePath,
                            String xCoordinateAttributeName,
                            String yCoordinateAttributeName
                            ) {
        this.treesFilePath = treesFilePath;
        this.burnIn = burnIn;
        this.numberOfIntervals = numberOfIntervals;
        this.traitName = traitName;
        this.rrwRateName = rrwRateName;
        this.hpdLevel = hpdLevel;
        this.gridSize = gridSize;
        this.mostRecentSamplingDate = mostRecentSamplingDate;
        this.timescaleMultiplier = timescaleMultiplier;
        // this.mccTreeFilePath = mccTreeFilePath;
        // this.xCoordinateAttributeName = xCoordinateAttributeName;
        // this.yCoordinateAttributeName = yCoordinateAttributeName;
    }

    private SpreadData parseContours()
        throws IOException, ImportException, SpreadException {

        double progress = 0;
        double progressStepSize = 0;
        this.updateProgress(progress);

        // --- parse trees ---//

        Double sliceHeights[] = null;
        if (this.sliceHeightsFilePath == null) {
            sliceHeights = generateSliceHeights(this.treesFilePath, this.numberOfIntervals);
        } else {
            SliceHeightsParser sliceHeightsParser = new SliceHeightsParser(this.sliceHeightsFilePath);
            sliceHeights = sliceHeightsParser.parseSliceHeights();
        }
        // sort them in ascending order
        Arrays.sort(sliceHeights);

        NexusImporter treesImporter = new NexusImporter(new FileReader(this.treesFilePath));
        HashMap<Double, List<double[]>> slicesMap = new HashMap<Double, List<double[]>>(sliceHeights.length);
        RootedTree currentTree;
        int treesReadCounter = 0;
        int totalTreesCount = getTreesCount(this.treesFilePath);
        int treesToSkip = (int) (burnIn * totalTreesCount);
        progressStepSize = 0.33 / totalTreesCount;

        while (treesImporter.hasTree()) {
            currentTree = (RootedTree) treesImporter.importNextTree();
            if (treesReadCounter >= treesToSkip) {
                new TimeSliceTree(slicesMap, //
                                  currentTree, //
                                  sliceHeights, //
                                  this.traitName, //
                                  this.rrwRateName).call();
            } // END: burnin check
            treesReadCounter++;
            progress += progressStepSize;
            this.updateProgress(progress);
        }

        // --- make contours ---//

        TimeParser timeParser = new TimeParser(this.mostRecentSamplingDate);
        LinkedList<Area> areasList = new LinkedList<Area>();

        progressStepSize = 0.33 / (double) slicesMap.size();
        for (Double sliceHeight : slicesMap.keySet()) {

            progress += progressStepSize;
            this.updateProgress(progress);

            List<double[]> coords = slicesMap.get(sliceHeight);
            int n = coords.size();

            double[] x = new double[n];
            double[] y = new double[n];

            for (int i = 0; i < n; i++) {
                x[i] = coords.get(i)[ParsersUtils.LONGITUDE_INDEX];
                y[i] = coords.get(i)[ParsersUtils.LATITUDE_INDEX];
            } // END: i loop

            ContourMaker contourMaker = new ContourWithSnyder(x, y, gridSize);
            ContourPath[] paths = contourMaker.getContourPaths(hpdLevel);

            for (ContourPath path : paths) {

                double[] longitude = path.getAllX();
                double[] latitude = path.getAllY();

                List<Coordinate> coordinateList = new ArrayList<Coordinate>();

                for (int i = 0; i < latitude.length; i++) {
                    coordinateList.add(new Coordinate(latitude[i], longitude[i]));
                }

                Polygon polygon = new Polygon(coordinateList);

                String startTime = timeParser.getNodeDate(sliceHeight * timescaleMultiplier);

                HashMap<String, Object> areaAttributesMap = new HashMap<String, Object>();
                areaAttributesMap.put(ParsersUtils.HPD.toUpperCase(), hpdLevel);

                Area area = new Area(polygon, startTime, areaAttributesMap);
                areasList.add(area);
            }
        }

        // --- collect attributes from areas --- //

        Map<String, Attribute> areasAttributesMap = new HashMap<String, Attribute>();

        progressStepSize = 0.33 / (double) areasList.size();
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

                        double value = ParsersUtils
                            .round(Double.valueOf(attributeValue.toString()), 100);

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
        } // END: points loop

        LinkedList<Attribute> uniqueAreaAttributes = new LinkedList<Attribute>();
        uniqueAreaAttributes.addAll(areasAttributesMap.values());

        Timeline timeline = timeParser.getTimeline(sliceHeights[sliceHeights.length - 1]);
        LinkedList<Layer> layersList = new LinkedList<Layer>();

        Layer contoursLayer = new Layer.Builder ().withAreas (areasList).build ();
        layersList.add(contoursLayer);

        return new SpreadData.Builder()
            .withTimeline(timeline)
            .withAreaAttributes(uniqueAreaAttributes)
            .withLayers(layersList)
            .build();
    }

    // private SpreadData parseMccTree() throws IOException, ImportException, SpreadException {
    //     if (this.mccTreeFilePath == null) {
    //         throw new SpreadException("mccTreeFilePath parameter was not set");
    //     } else if (this.xCoordinateAttributeName == null) {
    //         throw new SpreadException("xCoordinateAttributeName parameter was not set");
    //     } else if (this.yCoordinateAttributeName == null) {
    //         throw new SpreadException("yCoordinateAttributeName parameter was not set");
    //     } else {
    //         return new ContinuousTreeParser(this.mccTreeFilePath, this.xCoordinateAttributeName,
    //                                         this.yCoordinateAttributeName, "PLACEHOLDER", true, this.timescaleMultiplier,
    //                                         this.mostRecentSamplingDate).parseMccTree();
    //     }
    // }

    public String parse() throws IOException, ImportException, SpreadException {

        // SpreadData mccTree = this.parseMccTree();
        SpreadData contours = this.parseContours();

        LinkedList<Layer> layersList = new LinkedList<Layer>();

        Layer layer = new Layer.Builder ()
            // .withLines(mccTree.getLayers().get(0).getLines())
            // .withPoints(mccTree.getLayers().get(0).getPoints())
            .withAreas (contours.getLayers().get(0).getAreas())
            .build ();
        layersList.add(layer);

         SpreadData data = new SpreadData.Builder()
            .withAnalysisType(ParsersUtils.TIME_SLICER)
            .withTimeline(contours.getTimeline())
            .withAreaAttributes(contours.getAreaAttributes())
            .withLayers(layersList)
            .build();

        this.updateProgress(1.0);
        return new GsonBuilder().create().toJson(data);
    }

    private int getTreesCount(String file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {

            String mark = ";";
            int markCount = 0;
            int markBorder = 6;

            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;

            while ((readChars = is.read(c)) != -1) {

                empty = false;
                for (int i = 0; i < readChars; i++) {
                    if (String.valueOf((char) c[i]).equalsIgnoreCase(mark)) {
                        markCount++;
                    }

                    if (c[i] == '\n' && markCount > markBorder) {
                        count++;
                    }
                }
            }

            count = count - 1;
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
    }

    private Double[] generateSliceHeights(String treesFilePath, int numberOfIntervals)
        throws IOException, ImportException {

        Double[] timeSlices = new Double[numberOfIntervals];
        NexusImporter treesImporter = new NexusImporter(new FileReader(this.treesFilePath));
        double maxRootHeight = 0;
        RootedTree currentTree = null;

        while (treesImporter.hasTree()) {
            currentTree = (RootedTree) treesImporter.importNextTree();
            double rootHeight = currentTree.getHeight(currentTree.getRootNode());
            if (rootHeight > maxRootHeight) {
                maxRootHeight = rootHeight;
            }
        }

        for (int i = 0; i < numberOfIntervals; i++) {
            timeSlices[i] = maxRootHeight - (maxRootHeight / (double) numberOfIntervals) * ((double) i);
        }

        return timeSlices;
    }

    public String parseAttributes() throws IOException, ImportException, SpreadException {

        if (this.treesFilePath == null) {
            throw new SpreadException("treesFilePath parameter is not set", null);
        }

        NexusImporter treesImporter = new NexusImporter(new FileReader(this.treesFilePath));
        RootedTree tree = (RootedTree) treesImporter.importNextTree();

        Set<String> uniqueAttributes = tree.getNodes().stream().filter(node -> !tree.isRoot(node))
            .flatMap(node -> node.getAttributeNames().stream()).map(name -> name).collect(Collectors.toSet());

        return new GsonBuilder().create().toJson(uniqueAttributes);
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
