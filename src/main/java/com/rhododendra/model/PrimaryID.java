package com.rhododendra.model;

public abstract class PrimaryID {
    public static String primaryIdKey;

    public PrimaryID(String primaryID) {
        primaryIdKey = primaryID;
    }

    public abstract String primaryIdValue();
}
