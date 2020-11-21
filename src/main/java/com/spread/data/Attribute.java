package com.spread.data;

import java.util.HashSet;

public class Attribute {

	public static int MIN_INDEX = 0;
	public static int MAX_INDEX = 1;
	public static String LINEAR = "linear";
	public static String ORDINAL = "ordinal";

	private final String id;
	private final String scale;
	private Double[] range;
	private HashSet<Object> domain;

	public Attribute(String id, Double[] range) {

		this.id = id;
		this.scale = LINEAR;
		this.range = range;
		this.domain = null;
	}

	public Attribute(String id, HashSet<Object> domain) {

		this.id = id;
		this.scale = ORDINAL;
		this.range = null;
		this.domain = domain;
	}

	public String getScale() {
		return scale;
	}

	public Double[] getRange() {
		return range;
	}

	public HashSet<Object> getDomain() {
		return domain;
	}

	public String getId() {
		return id;
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Attribute other = (Attribute) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}// END: class
