package com.spread.data.attributable;

import java.util.Map;

import com.spread.data.primitive.Polygon;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString(includeFieldNames=true)
public class Area {

    @Getter
    private final String startTime;
    @Getter
    private final Map<String, Object> attributes;// = new LinkedHashMap<String, Object>();
    @Getter
    private final Polygon polygon;

    public Area( Polygon polygon, String startTime, Map<String, Object> attributes) {

        this.polygon = polygon;
        this.startTime = startTime;

        this.attributes = attributes;
        // if (attributes != null) {
        //     this.attributes.putAll(attributes);
        // }

    }

}
