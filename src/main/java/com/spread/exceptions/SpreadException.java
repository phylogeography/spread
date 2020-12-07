package com.spread.exceptions;

import org.joda.time.Instant;

public class SpreadException extends Exception {

    public static enum Type  {
        GENERAL_EXCEPTION, AUTHORIZATION_EXCEPTION, STORAGE_EXCEPTION
    }

    private static final long serialVersionUID = -1258021836219278875L;
    private final String message;
    private final String[][] meta;
    private final Type type;

    public SpreadException(Type type, String message, String[][] meta) {
        this.message = message;
        this.meta = meta;
        this.type = type;
    }

    public SpreadException(String message, String[][] meta) {
        this.message = message;
        this.meta = meta;
        this.type = Type.GENERAL_EXCEPTION;
    }

    public SpreadException(Type type, String message) {
        this.message = message;
        this.meta = new String[][] {
            {"timestamp", Instant.now().toString()},
        };
        this.type = type;
    }

    public SpreadException(String message) {
        this.message = message;
        this.type = Type.GENERAL_EXCEPTION;
        this.meta = new String[][] {
            {"timestamp", Instant.now().toString()},
        };
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
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
