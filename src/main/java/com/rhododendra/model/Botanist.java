package com.rhododendra.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Botanist implements PrimaryID {
    public static final String BOTANICAL_SHORT_KEY = "botanical_short";
    public static final String LOCATION_KEY = "location";
    public static final String BORN_DIED_KEY = "born_died";
    public static final String FULL_NAME_KEY = "full_name";
    public static final String IMAGE_KEY = "image";
    public static final String PRIMARY_ID_KEY = BOTANICAL_SHORT_KEY;

    @JsonProperty(BOTANICAL_SHORT_KEY)
    String botanicalShort;
    @JsonProperty(LOCATION_KEY)
    String location;
    @JsonProperty(BORN_DIED_KEY)
    String bornDied;
    @JsonProperty(FULL_NAME_KEY)
    String fullName;
    @JsonProperty(IMAGE_KEY)
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

    @JsonIgnore
    @Override
    public String getPrimaryID() {
        return getBotanicalShort();
    }
}
