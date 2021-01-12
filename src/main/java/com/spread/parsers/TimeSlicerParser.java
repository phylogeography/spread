package com.spread.parsers;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.spread.utils.ProgressBar;

import com.spread.exceptions.SpreadException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileReader;

import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.RootedTree;

import lombok.Setter;

public class TimeSlicerParser {

    @Setter
    private String treesFilePath;

    public TimeSlicerParser() {
    }


    public TimeSlicerParser(String treesFilePath, // path to the trees file
                            String trait // 2D trait for contouring
                            ) {

        this.treesFilePath = treesFilePath;

    }

    public String parse() throws IOException, ImportException
                                 // , SpreadException
    {

        // ---PARSE TREES---//

        int barLength = 100;
        int treesRead = 0;

        int assumedTrees = getAssumedTrees(this.treesFilePath);
        double stepSize = (double) barLength / (double) assumedTrees;

        System.out.println("Reading trees (bar assumes " + assumedTrees + " trees)");

        ProgressBar progressBar = new ProgressBar(barLength);
        progressBar.start();

        System.out.println("0                        25                       50                       75                       100%");
        System.out.println("|------------------------|------------------------|------------------------|------------------------|");


        // NexusImporter treesImporter;
        NexusImporter treesImporter = new NexusImporter(new FileReader(this.treesFilePath));

        RootedTree currentTree;
        ConcurrentHashMap<Double, List<double[]>> slicesMap = new ConcurrentHashMap<Double, List<double[]>>();

        // Executor for threads
        int NTHREDS = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

        int counter = 0;
        while (treesImporter.hasTree()) {

            // try {

                currentTree = (RootedTree) treesImporter.importNextTree();

            // } catch (Exception e) {
            //     // catch any unchecked exceptions coming from Runnable, pass
            //     // them to handlers
            //     // throw new SpreadException(e.getMessage());
            // } // END: try-catch

        }






        return "";
    }

    public static int getAssumedTrees(String file) throws IOException {
        // this method is a hack

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

            } // END: loop

            count = count - 1;
            return (count == 0 && !empty) ? 1 : count;

        } finally {
            is.close();
        }
    }// END: getAssumedTrees

}
