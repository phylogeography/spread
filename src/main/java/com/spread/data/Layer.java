package com.spread.data;

import java.util.List;

import com.spread.data.attributable.Area;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.ToString;

@ToString(includeFieldNames=true)
public class Layer {

    // public enum Type {
    //     tree, counts, trees
    // }

    @Getter
    private final List<Point> points;

    @Getter
    private final List<Line> lines;

    @Getter
    private final List<Area> areas;

    public static class Builder {

        private List<Point> points;
        private List<Line> lines;
        private List<Area> areas;

        public Builder(
                       // Type type
                       ) {
            // this.type=type
        }

        public Builder withPoints(List<Point> points) {
            this.points = points;
            return this;
        }

        public Builder withLines(List<Line> lines) {
            this.lines = lines;
            return this;
        }

        public Builder withAreas(List<Area> areas) {
            this.areas = areas;
            return this;
        }

        public Layer build() {
            Layer layer = new Layer (this);
            return layer;
        }

    }

    private Layer(Builder builder) {
        this.points = builder.points;
        this.lines = builder.lines;
        this.areas = builder.areas;
    }

    // public enum Type {
    //     tree, counts, trees
    // }

    // @Getter
    // private final Type type;
    // @Getter
    // private final String id;
    // @Getter
    // private final String description;

    // @Getter
    // private final List<Point> points;
    // @Getter
    // private final List<Line> lines;
    // @Getter
    // private final List<Area> areas;

    // @Accessors(fluent = true)
    // @Getter
    // private final boolean hasPoints;
    // @Accessors(fluent = true)
    // @Getter
    // private final boolean hasLines;
    // @Accessors(fluent = true)
    // @Getter
    // private final boolean hasAreas;

    // public Layer(String id,
    //              String description,
    //              List<Point> points,
    //              List<Line> lines,
    //              List<Area> areas) {
    //     this.type = Type.tree;
    //     this.id = id;
    //     this.description = description;
    //     this.points = points;
    //     this.lines = lines;
    //     this.areas = areas;
    //     this.hasPoints = true;
    //     this.hasLines = true;
    //     this.hasAreas = true;
    // }

    // public Layer(String id,
    //              String description,
    //              List<Point> points,
    //              List<Line> lines) {
    //     this.type = Type.tree;
    //     this.id = id;
    //     this.description = description;
    //     this.points = points;
    //     this.lines = lines;
    //     this.areas = null;
    //     this.hasPoints = true;
    //     this.hasLines = true;
    //     this.hasAreas = false;
    // }

    // public <T> Layer(String id,
    //              String description,
    //              List<T> collection) {
    //     this.type = Type.counts;
    //     this.id = id;
    //     this.description = description;
    //     // this.points = points;
    //     this.lines = null;
    //     this.areas = null;
    //     this.hasPoints = true;
    //     this.hasLines = false;
    //     // this.hasAreas = false;

    //     // if (T instanceof Area) {

    //     // }

    // }

    // // public  Layer(String id,
    // //              String description,
    // //              List<Point> points) {
    // //     this.type = Type.counts;
    // //     this.id = id;
    // //     this.description = description;
    // //     this.points = points;
    // //     this.lines = null;
    // //     this.areas = null;
    // //     this.hasPoints = true;
    // //     this.hasLines = false;
    // //     this.hasAreas = false;
    // // }

    // // public Layer(String id,
    // //              String description,
    // //              List<Area> areas) {
    // //     this.type = Type.trees;
    // //     this.id = id;
    // //     this.description = description;
    // //     this.areas = areas;
    // //     this.hasPoints = false;
    // //     this.hasLines = false;
    // //     this.hasAreas = true;
    // // }

}
