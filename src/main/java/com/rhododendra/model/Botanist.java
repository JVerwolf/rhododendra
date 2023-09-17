package com.rhododendra.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Botanist implements PrimaryID {
    @JsonProperty("botanical_short") String botanicalShort;
    String location;
    @JsonProperty("born_died")String bornDied;
    @JsonProperty("full_name")String fullName;
    String image;

    public String getBotanicalShort() {
        return botanicalShort;
    }

    public void setBotanicalShort(String botanicalShort) {
        this.botanicalShort = botanicalShort;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBornDied() {
        return bornDied;
    }

    public void setBornDied(String bornDied) {
        this.bornDied = bornDied;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String getPrimaryID() {
        return getBotanicalShort();
    }
}
