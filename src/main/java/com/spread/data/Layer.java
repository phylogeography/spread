package com.spread.data;

import java.util.List;

import com.spread.data.attributable.Area;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;

import lombok.Getter;
import lombok.ToString;

@ToString(includeFieldNames=true)
public class Layer {

    @Getter
    private final List<Point> points;
    @Getter
    private final List<Line> lines;
    @Getter
    private final List<Area> areas;
    @Getter
    private List<Point> counts;

    public static class Builder {

        private List<Point> points;
        private List<Line> lines;
        private List<Area> areas;
        private List<Point> counts;

        public Builder() {
        }

        public Builder withCounts(List<Point> counts) {
            this.counts = counts;
            return this;
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
        this.counts = builder.counts;
        this.points = builder.points;
        this.lines = builder.lines;
        this.areas = builder.areas;
    }

}
