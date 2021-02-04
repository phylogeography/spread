package com.spread.data;

import lombok.ToString;
import lombok.Getter;

/**
 * @author Filip Bielejec
 */
@ToString(includeFieldNames=true)
public class AxisAttributes {

        @Getter
        private final String xCoordinate;
        @Getter
        private final String yCoordinate;

        public AxisAttributes(String xCoordinate, String yCoordinate) {
                this.xCoordinate = xCoordinate;
                this.yCoordinate = yCoordinate;
        }

}
