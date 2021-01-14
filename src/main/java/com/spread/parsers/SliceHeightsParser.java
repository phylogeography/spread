package com.spread.parsers;

import java.io.IOException;

import com.spread.utils.ParsersUtils;

public class SliceHeightsParser {

    private String sliceHeights;

    public SliceHeightsParser(String sliceHeights) {
        this.sliceHeights = sliceHeights;
    }

    public Double[] parseSliceHeights() throws IOException {

        String[] lines = ParsersUtils.readLines(sliceHeights, ParsersUtils.HASH_COMMENT);
        int nrow = lines.length;

        Double[] sliceHeights = new Double[nrow];
        for (int i = 0; i < nrow; i++) {
            sliceHeights[i] = Double.parseDouble(lines[i]);
        }

        return sliceHeights;
    }

}
