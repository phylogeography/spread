package com.spread.data;

import java.util.HashSet;

import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@ToString(includeFieldNames=true)
public class Attribute {

        public static int MIN_INDEX = 0;
        public static int MAX_INDEX = 1;
        public static String LINEAR = "linear";
        public static String ORDINAL = "ordinal";

        @Getter
        private final String id;
        @Getter
        private final String scale;
        @Getter
        private Double[] range;
        @Getter
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

}
