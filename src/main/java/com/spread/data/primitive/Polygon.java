package com.spread.data.primitive;

import java.util.List;

import lombok.ToString;
import lombok.Getter;

@ToString(includeFieldNames=true)
public class Polygon {

    @Getter
    private final List<Coordinate> coordinates;

    public Polygon(List<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }

}
