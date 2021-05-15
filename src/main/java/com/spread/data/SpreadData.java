package com.spread.data;

import java.util.List;

import com.spread.data.attributable.Area;
import com.spread.data.attributable.Line;
import com.spread.data.attributable.Point;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Filip Bielejec
 */
@ToString(includeFieldNames=true)
public class SpreadData {

    @Getter
    private String analysisType;
    @Getter
    private final Timeline timeline;
    @Getter
    private final AxisAttributes axisAttributes;

    // TODO : use Set for unique attributes below
    @Getter
    private final List<Attribute> lineAttributes;
    @Getter
    private final List<Attribute> pointAttributes;
    @Getter
    private final List<Attribute> areaAttributes;
    @Getter
    private final List<Location> locations;

    @Getter
    private final List<Point> points;
    @Getter
    private final List<Line> lines;
    @Getter
    private final List<Area> areas;
    @Getter
    private final List<Point> counts;

    public static class Builder {

        private String analysisType;
        private Timeline timeline;
        private AxisAttributes axisAttributes;
        private List<Attribute> lineAttributes;
        private List<Attribute> pointAttributes;
        private List<Attribute> areaAttributes;
        private List<Location> locations;

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

        public Builder withAnalysisType(String analysisType) {
            this.analysisType = analysisType;
            return this;
        }

        public Builder withTimeline(Timeline timeline) {
            this.timeline = timeline;
            return this;
        }

        public Builder withAxisAttributes(AxisAttributes axisAttributes) {
            this.axisAttributes = axisAttributes;
            return this;
        }

        public Builder withLineAttributes(List<Attribute> lineAttributes) {
            this.lineAttributes = lineAttributes;
            return this;
        }

        public Builder withPointAttributes(List<Attribute> pointAttributes) {
            this.pointAttributes = pointAttributes;
            return this;
        }

        public Builder withAreaAttributes(List<Attribute> areaAttributes) {
            this.areaAttributes = areaAttributes;
            return this;
        }

        public Builder withLocations(List<Location> locations) {
            this.locations = locations;
            return this;
        }

        public SpreadData build() {
            SpreadData data = new SpreadData (this);
            return data;
        }

    }

    private SpreadData(Builder builder) {
        this.analysisType = builder.analysisType;
        this.timeline = builder.timeline;
        this.axisAttributes = builder.axisAttributes;
        this.lineAttributes = builder.lineAttributes;
        this.pointAttributes = builder.pointAttributes;
        this.areaAttributes = builder.areaAttributes;
        this.locations = builder.locations;
        this.points = builder.points;
        this.lines = builder.lines;
        this.counts = builder.counts;
        this.areas = builder.areas;
    }

}
