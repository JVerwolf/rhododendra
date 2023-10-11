package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class PrimaryID {

    @JsonIgnore
    public abstract String primaryIdValue();
}
