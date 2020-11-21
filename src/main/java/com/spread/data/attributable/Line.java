package com.spread.data.attributable;

import java.util.LinkedHashMap;
import java.util.Map;

public class Line {

	private final String startPointId;
	private final String endPointId;

	private final String startTime;
	private final String endTime;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	public Line(String startPointId, //
			String endPointId, //
			String startTime, //
			String endTime, //
			Map<String, Object> attributes //
	) {

		this.startPointId = startPointId;
		this.endPointId = endPointId;

		this.startTime = startTime;
		this.endTime = endTime;

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}

	}// END: Constructor

	public Line(String startPointId, //
			String endPointId, //
			Map<String, Object> attributes //
	) {

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

	public String getStartTime() {
		return startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public String getStartPointId() {
		return startPointId;
	}

	public String getEndPointId() {
		return endPointId;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

}// END: class
