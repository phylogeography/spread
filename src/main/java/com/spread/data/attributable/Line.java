package com.spread.data.attributable;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@ToString(includeFieldNames=true)
public class Line {

    @Getter
    private final String startPointId;
    @Getter
    private final String endPointId;
    @Getter
    private final String startTime;
    @Getter
    private final String endTime;
    @Getter
    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    public Line(String startPointId,
                String endPointId,
                String startTime,
                String endTime,
                Map<String, Object> attributes) {
        this.startPointId = startPointId;
        this.endPointId = endPointId;
        this.startTime = startTime;
        this.endTime = endTime;
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }

    public Line(String startPointId,
                String endPointId,
                Map<String, Object> attributes) {
        this.startPointId = startPointId;
        this.endPointId = endPointId;
        this.startTime = null;
        this.endTime = null;
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }

    public boolean hasTime() {
        boolean hasTime = false;
        if (startTime != null) {
            hasTime = true;
        }
        return hasTime;
    }

}
