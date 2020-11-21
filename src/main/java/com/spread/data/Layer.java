package com.spread.data;

import java.util.List;

import com.spread.data.attributable.Area;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;
// import com.spread.data.geojson.GeoJsonData;

public class Layer {

        public enum Type {
                map, tree, counts
        }

        private final Type type;
        private final String id;
        private final String description;

        private final List<Point> points;
        private final List<Line> lines;
        private final List<Area> areas;

        // private final GeoJsonData geojson;

        // private boolean hasAreas;

        public Layer(String id, //
                        String description, //
                        List<Point> points, //
                        List<Line> lines, //
                        List<Area> areas //
        ) {

                this.type = Type.tree;

                this.id = id;
                this.description = description;

                this.points = points;
                this.lines = lines;
                this.areas = areas;

                // this.geojson = null;

        }// END: Constructor

        public Layer(String id, //
                        String description, //
                        List<Point> points, //
                        List<Line> lines //
        ) {

                this.type = Type.tree;

                this.id = id;
                this.description = description;

                this.points = points;
                this.lines = lines;
                this.areas = null;
                // this.hasAreas = false;

                // this.geojson = null;

        }// END: Constructor

        public Layer(String id, //
                        String description, //
                        List<Point> points //
        ) {

                this.type = Type.counts;

                this.id = id;
                this.description = description;

                this.points = points;
                this.lines = null;
                this.areas = null;
                // this.hasAreas = false;

                // this.geojson = null;

        }// END: Constructor

        // public Layer(
        // String id, //
        // String description, //
        // List<Area> areas //
        // ) {
        //
        // this.type = Type.data;
        // this.id = id;
        // this.description = description;
        //
        //
        // this.points = null;
        // this.lines = null;
        // this.areas = areas;
        // this.hasAreas = true;
        //
        // this.geojson = null;
        //
        // }//END: Constructor

        public Layer(String id, //
                        String description //
                        // GeoJsonData geojson //
        ) {

                this.type = Type.map;
                this.id = id;
                this.description = description;
                // this.geojson = geojson;

                this.points = null;
                this.lines = null;
                this.areas = null;
                // this.hasAreas = false;

        }// END: Constructor

        public List<Line> getLines() {
                return lines;
        }

        public boolean hasLines() {

                boolean hasLines = false;
                if (this.lines != null) {
                        hasLines = true;
                }

                return hasLines;
        }

        public List<Area> getAreas() {
                return areas;
        }

        public boolean hasAreas() {

                boolean hasAreas = false;
                if (this.areas != null) {
                        hasAreas = true;
                }

                return hasAreas;
        }

        public List<Point> getPoints() {
                return points;
        }

        public boolean hasPoints() {

                boolean hasPoints = false;
                if (this.points != null) {
                        hasPoints = true;
                }

                return hasPoints;
        }

        public String getId() {
                return id;
        }

        public String getDescription() {
                return description;
        }

        public String getType() {
                return type.toString();
        }

        // public GeoJsonData getGeojson() {
        //         return geojson;
        // }

}// END: class
