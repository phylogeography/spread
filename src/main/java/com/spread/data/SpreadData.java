package com.spread.data;

import java.util.LinkedList;
import lombok.Getter;

/**
 * @author Filip Bielejec
 */
public class SpreadData {

    @Getter
    private final TimeLine timeLine;
    @Getter
    private final AxisAttributes axisAttributes;
    @Getter
    private final LinkedList<Attribute> lineAttributes;
    @Getter
    private final LinkedList<Attribute> pointAttributes;
    @Getter
    private final LinkedList<Attribute> areaAttributes;
    @Getter
    private final LinkedList<Location> locations;
    @Getter
    private final LinkedList<Layer> layers;

    public SpreadData(TimeLine timeLine,
                      AxisAttributes axisAttributes,
                      LinkedList<Attribute> lineAttributes,
                      LinkedList<Attribute> pointAttributes,
                      LinkedList<Attribute> areaAttributes,
                      LinkedList<Location> locations,
                      LinkedList<Layer> layers) {
        this.timeLine = timeLine;
        this.axisAttributes = axisAttributes;
        this.lineAttributes = lineAttributes;
        this.pointAttributes = pointAttributes;
        this.areaAttributes = areaAttributes;
        this.locations = locations;
        this.layers = layers;
    }

    public boolean hasLocations() {
        return locations != null ? true : false;
    }

}
