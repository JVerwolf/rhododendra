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
        public String resolvedNormalURL;
        public String resolvedHiResURL;
        public String resolvedTagURL;
        public PhotoDetails photoDetails;

        public PhotoRowInfo(
            String resolvedNormalURL,
            String resolvedHiResURL,
            String resolvedTagURL,
            PhotoDetails photoDetails
        ) {
            this.resolvedNormalURL = resolvedNormalURL;
            this.resolvedHiResURL = resolvedHiResURL;
            this.resolvedTagURL = resolvedTagURL;
            this.photoDetails = photoDetails;
        }

        public String getResolvedNormalURL() {
            return resolvedNormalURL;
        }

        public String getResolvedHiResURL() {
            return resolvedHiResURL;
        }

        public String getResolvedTagURL() {
            return resolvedTagURL;
        }

        public PhotoDetails getPhotoDetails() {
            return photoDetails;
        }
    }
}
