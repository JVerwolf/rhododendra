package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Genetics {
    String seed_parent;
    String seed_parent_id;
    String pollen_parent;
    String pollen_parent_id;

    public String getSeed_parent() {
        return seed_parent;
    }

    public void setSeed_parent(String seed_parent) {
        this.seed_parent = seed_parent;
    }

    public String getSeed_parent_id() {
        return seed_parent_id;
    }

    public void setSeed_parent_id(String seed_parent_id) {
        this.seed_parent_id = seed_parent_id;
    }

    public String getPollen_parent() {
        return pollen_parent;
    }

    public void setPollen_parent(String pollen_parent) {
        this.pollen_parent = pollen_parent;
    }

    public String getPollen_parent_id() {
        return pollen_parent_id;
    }

    public void setPollen_parent_id(String pollen_parent_id) {
        this.pollen_parent_id = pollen_parent_id;
    }
}
