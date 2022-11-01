package com.tdx.AndroidNew;


public enum CodeBlockType {
    UNKNOWN("unknown", 0),
    USED("used", 1),
    FAKE("fake", 2);

    private String name;
    private int code;

    CodeBlockType(String name, int code) {
        this.name = name;
        this.code = code;
    }
    public String toString() {
        return this.name;
    }
    public int getCode() {
        return this.code;
    }
}

