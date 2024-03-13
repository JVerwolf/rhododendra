package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PHOTO_DETAILS")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhotoDetails extends Indexable {
    public static final String PHOTO_BY = "photo_by";
    public static final String HI_RES_PHOTO = "hi_res_photo";
    public static final String PHOTO = "photo";

    public static final String PRIMARY_ID_KEY = PHOTO;

    @JsonProperty(PHOTO_BY)
    String photoBy;
    String date;
    String location;
    @JsonProperty(HI_RES_PHOTO)
    String hiResPhoto;
    @Id
    @JsonProperty(PHOTO)
    String photo;
    String description;
    String name;
    String tag;

    public PhotoDetails(){}
    public PhotoDetails(
        String photoBy,
        String date,
        String location,
        String hiResPhoto,
        String photo,
        String description,
        String name,
        String tag
    ) {
        this.photoBy = photoBy;
        this.date = date;
        this.location = location;
        this.hiResPhoto = hiResPhoto;
        this.photo = photo;
        this.description = description;
        this.name = name;
        this.tag = tag;
    }

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
