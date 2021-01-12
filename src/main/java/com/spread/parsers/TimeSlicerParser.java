package com.spread.parsers;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.spread.utils.PrintUtils;
import com.spread.utils.ProgressBar;

import com.spread.exceptions.SpreadException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;

import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.RootedTree;

import lombok.Setter;

public class TimeSlicerParser {

    @Setter
    private String treesFilePath;

    @Setter
    private String sliceHeightsFilePath;

    @Setter
    private int numberOfIntervals;

    @Setter
    private int burnIn;

    @Setter
    private String traitName;

    @Setter
    private String rrwRateName;

    public TimeSlicerParser() {
    }

    public TimeSlicerParser(String treesFilePath, // path to the trees file
                            int burnIn, //
                            String sliceHeightsFilePath,
                            String traitName, // 2D trait for contouring
                            String rrwRateName // 2D trait rate attribute
                            ) {
        this.treesFilePath = treesFilePath;
        this.sliceHeightsFilePath = sliceHeightsFilePath;
        this.burnIn = burnIn;
        this.traitName = traitName;
        this.rrwRateName = rrwRateName;

    }

    public TimeSlicerParser(String treesFilePath,
                            int burnIn,
                            int numberOfIntervals,
                            String traitName,
                            String rrwRateName
                            ) {
        this.treesFilePath = treesFilePath;
        this.burnIn = burnIn;
        this.numberOfIntervals = numberOfIntervals;
        this.traitName = traitName;
        this.rrwRateName = rrwRateName;

    }

    public String parse() throws IOException, ImportException
                                 // , SpreadException
    {

        int barLength = 100;
        int assumedTrees = getAssumedTrees(this.treesFilePath);
        double stepSize = (double) barLength / (double) assumedTrees;

        System.out.println("Reading trees (bar assumes " + assumedTrees + " trees)");

        ProgressBar progressBar = new ProgressBar(barLength);
        progressBar.start();

        System.out.println("0                        25                       50                       75                       100%");
        System.out.println("|------------------------|------------------------|------------------------|------------------------|");

        // NexusImporter treesImporter;
        NexusImporter treesImporter = new NexusImporter(new FileReader(this.treesFilePath));

        ConcurrentHashMap<Double, List<double[]>> slicesMap = new ConcurrentHashMap<Double, List<double[]>>();

        // Executor for threads
        int NTHREDS = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

        Double sliceHeights[] = null;
        if (this.sliceHeightsFilePath == null) {
            sliceHeights = generateSliceHeights(this.treesFilePath, this.numberOfIntervals);
        } else {
            SliceHeightsParser sliceHeightsParser = new SliceHeightsParser(this.sliceHeightsFilePath);
            sliceHeights = sliceHeightsParser.parseSliceHeights();
        }

        // sort them in ascending order
        Arrays.sort(sliceHeights);

        System.out.println("Using as slice heights: ");
        PrintUtils.printArray(sliceHeights);

        RootedTree currentTree = null;
        int treesRead = 0;
        int counter = 0;
        while (treesImporter.hasTree()) {

            try {

                currentTree = (RootedTree) treesImporter.importNextTree();

                if (counter >= burnIn) {

                    executor.submit(new TimeSliceTree(slicesMap, //
                                                      currentTree, //
                                                      sliceHeights, //
                                                      this.traitName, //
                                                      this.rrwRateName //
                                                      ));

                    treesRead++;
                } // END: burnin check

                counter++;
                double progress = (stepSize * counter) / barLength;
                progressBar.setProgressPercentage(progress);





            } catch (Exception e) {
                // catch any unchecked exceptions coming from Runnable, pass
                // them to handlers
                // throw new SpreadException(e.getMessage());
            } // END: try-catch

        }

        // Wait until all threads are finished
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        progressBar.showCompleted();
        progressBar.setShowProgress(false);

        System.out.print("\n");
        System.out.println("Analyzed " + treesRead + " trees with burn-in of " + burnIn + " for the total of " + counter
                           + " trees");


        // TODO return JSON
        return "";
    }

    public static int getAssumedTrees(String file) throws IOException {
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

    private Double[] generateSliceHeights(String treesFilePath, int numberOfIntervals) throws IOException, ImportException {
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

}
