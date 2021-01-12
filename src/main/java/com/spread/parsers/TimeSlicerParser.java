package com.spread.parsers;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import lombok.Setter;

public class TimeSlicerParser {

    @Setter
    private String treesFilePath;

    public TimeSlicerParser() {
    }


    public TimeSlicerParser(String treesFilePath, // path to the trees file
                            String trait // 2D trait for contouring
                            ) {

    }

    public String parse() throws IOException {

        // ---PARSE TREES---//

        int barLength = 100;
        int treesRead = 0;

        int assumedTrees = getAssumedTrees(this.treesFilePath);
        double stepSize = (double) barLength / (double) assumedTrees;

        System.out.println("Reading trees (bar assumes " + assumedTrees + " trees)");


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
