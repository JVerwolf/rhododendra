package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Indexable {

    @JsonIgnore
    public abstract String primaryIdValue();
}
