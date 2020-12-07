package com.spread.data;

import com.spread.data.primitive.Coordinate;
import com.spread.data.primitive.Polygon;

public class Location {

    private final String id;
    private final Polygon polygon;
    private final Coordinate coordinate;

    public Location(String id) {
        this.id = id;
        this.coordinate = null;
        this.polygon = null;
    }

    public Location(String id,
                    Polygon polygon) {

        super();

        this.id = id;
        this.coordinate = null;
        this.polygon = polygon;
    }

    public Location(String id,
                    Coordinate coordinate) {

        super();

        this.id = id;
        this.coordinate = coordinate;
        this.polygon = null;

    }

    public String getId() {
        return id;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public boolean hasCoordinate()  {
        return coordinate == null ? false : true;
    }

    public Coordinate getCoordinate()  {
        return coordinate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Location)) {
            return false;
        }
        Location location = (Location) obj;
        if (location.getId().equalsIgnoreCase(this.id)) {
            return true;
        } else {
            return false;
        }
    }

}
