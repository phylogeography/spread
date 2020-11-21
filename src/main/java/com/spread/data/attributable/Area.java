package com.spread.data.attributable;

import java.util.LinkedHashMap;
import java.util.Map;

import com.spread.data.primitive.Polygon;

public class Area {
	
	private final String startTime;
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	private final Polygon polygon;
	
	public Area( Polygon polygon, String startTime, Map<String, Object> attributes) {
		
		this.polygon = polygon;
		this.startTime = startTime;
		
		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
		
	}//END: Constructor
	
	public Polygon getPolygon() {
		return polygon;
	}

	public String getStartTime() {
		return startTime;
	}
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	
}//END: class
