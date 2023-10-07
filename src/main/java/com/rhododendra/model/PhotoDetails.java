package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhotoDetails extends PrimaryID {
    public static final String PHOTO_BY = "photo_by";
    public static final String DATE = "date";
    public static final String LOCATION = "location";
    public static final String HI_RES_PHOTO = "hi_res_photo";
    public static final String PHOTO = "photo";
    public static final String DESCRIPTION = "description";
    public static final String NAME = "name";
    public static final String TAG = "tag";

    public static final String PRIMARY_ID_KEY = PHOTO;

    @JsonProperty(PHOTO_BY)
    String photoBy;
    @JsonProperty(DATE)
    String date;
    @JsonProperty(LOCATION)
    String location;
    @JsonProperty(HI_RES_PHOTO)
    String hiResPhoto;
    @JsonProperty(PHOTO)
    String photo;
    @JsonProperty(DESCRIPTION)
    String description;
    @JsonProperty(NAME)
    String name;
    @JsonProperty(TAG)
    String tag;

    @Override
    public String primaryIdValue() {
        return photo;
    }

    public String getPhotoBy() {
        return photoBy;
    }

    public void setPhotoBy(String photoBy) {
        this.photoBy = photoBy;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getHiResPhoto() {
        return hiResPhoto;
    }

    public void setHiResPhoto(String hiResPhoto) {
        this.hiResPhoto = hiResPhoto;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
