package com.rhododendra.model;

import java.util.List;

public class SpeciesDetail {
    private List<PhotoRowInfo> photoRowInfoList;
    private Species species;

    public SpeciesDetail(Species species, List<PhotoRowInfo> photoRowInfoList) {
        this.photoRowInfoList = photoRowInfoList;
        this.species = species;
    }

    public List<PhotoRowInfo> getPhotoRowInfoList() {
        return photoRowInfoList;
    }

    public Species getSpecies() {
        return species;
    }

    public static class PhotoRowInfo {
        public String resolvedURL;
        public PhotoDetails photoDetails;

        public PhotoRowInfo(String resolvedURL, PhotoDetails photoDetails) {
            this.resolvedURL = resolvedURL;
            this.photoDetails = photoDetails;
        }

        public String getResolvedURL() {
            return resolvedURL;
        }

        public PhotoDetails getPhotoDetails() {
            return photoDetails;
        }
    }
}
