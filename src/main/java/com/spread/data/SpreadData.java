package com.spread.data;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Filip Bielejec
 */
public class SpreadData {

        private final TimeLine timeLine;
        private final AxisAttributes axisAttributes;
        // private final LinkedList<Attribute> mapAttributes;
        private final LinkedList<Attribute> lineAttributes;
        private final LinkedList<Attribute> pointAttributes;
        private final LinkedList<Attribute> areaAttributes;
        private final LinkedList<Location> locations;
        private final LinkedList<Layer> layers;

    public SpreadData(TimeLine timeLine, //
                      AxisAttributes axisAttributes,
                      // LinkedList<Attribute> mapAttributes, //
                      LinkedList<Attribute> lineAttributes, //
                      LinkedList<Attribute> pointAttributes, //
                      LinkedList<Attribute> areaAttributes, //
                      LinkedList<Location> locations, //
                      LinkedList<Layer> layers) {

                this.timeLine = timeLine;
                this.axisAttributes = axisAttributes;
                // this.mapAttributes = mapAttributes;
                this.lineAttributes = lineAttributes;
                this.pointAttributes = pointAttributes;
                this.areaAttributes = areaAttributes;
                this.locations = locations;
                this.layers = layers;

        }// END: Constructor

        public List<Layer> getLayers() {
                return layers;
        }

        public TimeLine getTimeLine() {
                return timeLine;
        }

        public boolean hasLocations() {
                return locations != null ? true : false;
        }

        public LinkedList<Location> getLocations() {
                return locations;
        }

        // public LinkedList<Attribute> getMapAttributes() {
        //         return mapAttributes;
        // }

        public LinkedList<Attribute> getLineAttributes() {
                return lineAttributes;
        }

        public LinkedList<Attribute> getPointAttributes() {
                return pointAttributes;
        }

        public AxisAttributes getAxisAttributes() {
                return axisAttributes;
        }

        public LinkedList<Attribute> getAreaAttributes() {
                return areaAttributes;
        }

}
