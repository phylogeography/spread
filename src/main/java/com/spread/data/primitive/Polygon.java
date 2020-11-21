package com.spread.data.primitive;

import java.util.List;

public class Polygon {

	private final List<Coordinate> coordinates;
	private final double altitude;

	public Polygon(List<Coordinate> coordinates, //
			double altitude //
	) {

		this.coordinates = coordinates;
		this.altitude = altitude;

	}// END: Constructor

	public Polygon(List<Coordinate> coordinates) {

		this.coordinates = coordinates;
		this.altitude = 0;

	}// END: Constructor

	public List<Coordinate> getCoordinates() {
		return coordinates;
	}

	public double getAltitude() {
		return altitude;
	}

}// END: class
