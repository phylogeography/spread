package com.spread.exceptions;

import org.joda.time.Instant;

public class SpreadException extends Exception {

    private static final long serialVersionUID = -1258021836219278875L;
    private final String message;
    private final String[][] meta;

    public SpreadException(String message, String[][] meta) {
        this.message = message;
        this.meta = meta;
    }

    public SpreadException(String message) {
        this.message = message;
        this.meta = new String[][] {
            {"timestamp", Instant.now().toString()},
        };
    }

    /**
     * @return the meta
     */
    public String[][] getMeta() {
        return meta;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
