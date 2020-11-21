package com.spread.data.primitive;

public class Coordinate {

    private final Double xCoordinate;
    private final Double yCoordinate;
    private Double altitude;

    public Coordinate(Double latitude,
                      Double longitude,
                      Double altitude) {
        this.xCoordinate = longitude;
        this.yCoordinate = latitude;
        this.altitude = altitude;
    }

    public Coordinate(Double latitude,
                      Double longitude) {
        this.xCoordinate = longitude;
        this.yCoordinate = latitude;
        this.altitude = 0.0;
    }

    public double getXCoordinate() {
        return xCoordinate;
    }

    public double getYCoordinate() {
        return yCoordinate;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((xCoordinate == null) ? 0 : xCoordinate.hashCode());
        result = prime * result + ((yCoordinate == null) ? 0 : yCoordinate.hashCode());
        return result;
    }

}
