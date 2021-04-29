package com.spread.data;

import java.util.List;

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
    private final List<Layer> layers;

    public static class Builder {

        private Timeline timeline;
        private AxisAttributes axisAttributes;
        private List<Attribute> lineAttributes;
        private List<Attribute> pointAttributes;
        private List<Attribute> areaAttributes;
        private List<Location> locations;
        private List<Layer> layers;
        private String analysisType;

        public Builder() {
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

        public Builder withLayers(List<Layer> layers) {
            this.layers = layers;
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
        this.layers = builder.layers;
    }

}
