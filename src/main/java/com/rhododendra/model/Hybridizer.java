package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Hybridizer extends Indexable {
    public static final String PRIMARY_ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String NAME_KEY_FOR_SORT = "name_key_for_sort";


    @JsonIgnore
    private Long id;
    @JsonProperty("id")
    private String oldId;
    private String name;
    private String location;
    private List<String> photos;

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public String getOldId() {
        return oldId;
    }

    public void setOldId(String oldId) {
        this.oldId = oldId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    @JsonIgnore
    @Override
    public String primaryIdValue() {
        return getId() == null ? "" : String.valueOf(getId());
    }
}
