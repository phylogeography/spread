package com.spread.data.attributable;

import java.util.LinkedHashMap;
import java.util.Map;

import com.spread.data.Location;
import com.spread.data.primitive.Coordinate;

public class Point {

	private final String id;
	private final String locationId;
	private final Coordinate coordinate;
	private final String startTime;
	private final String endTime;
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	public Point(String locationId) {
		this.locationId = locationId;
		this.coordinate = null;
		this.startTime = null;
		this.endTime = null;

		this.id = String.valueOf(Math.abs(locationId.hashCode()));

	}

	public Point(String locationId, String startTime, Map<String, Object> attributes) {

		this.locationId = locationId;
		this.coordinate = null;
		this.startTime = startTime;
		this.endTime = null;

		this.id = String.valueOf(Math.abs(locationId.hashCode() + startTime.hashCode()));

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}

	}// END: Constructor

	public Point(String locationId, String startTime, String endTime, Map<String, Object> attributes) {

		this.locationId = locationId;
		this.coordinate = null;
		this.startTime = startTime;
		this.endTime = endTime;

		this.id = String.valueOf(Math.abs(locationId.hashCode() + startTime.hashCode() + endTime.hashCode()));

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}

	}// END: Constructor

	public Point(Coordinate coordinate, String startTime, Map<String, Object> attributes) {

		this.coordinate = coordinate;
		this.locationId = null;
		this.startTime = startTime;
		this.endTime = null;

		this.id = String.valueOf(Math.abs(coordinate.hashCode() + startTime.hashCode()));

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}// END:
		// Constructor

	public String getId() {
		return id;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public boolean hasLocationId() {
		boolean hasLocation = false;
		if (this.locationId != null) {
			hasLocation = true;
		}

		return hasLocation;
	}// END: hasLocationId

	public String getLocationId() {
		return locationId;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void addAttribute(String name, Object value) {
		attributes.put(name, value);
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

		Point point = (Point) obj;
		if (point.getId().equals(this.id)) {
			return true;
		} else {
			return false;
		}

	}// END: equals

}// END: class
