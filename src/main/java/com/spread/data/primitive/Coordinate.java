package com.spread.data.primitive;

import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(includeFieldNames=true)
public class Coordinate {

    @Getter
    @EqualsAndHashCode.Include
    private final Double xCoordinate;
    @Getter
    @EqualsAndHashCode.Include
    private final Double yCoordinate;

    public Coordinate(Double latitude,
                      Double longitude) {
        this.xCoordinate = longitude;
        this.yCoordinate = latitude;
    }

	public Double getX(){
		return xCoordinate;
	}

	public Double getY(){
		return yCoordinate;
	}
	

}
