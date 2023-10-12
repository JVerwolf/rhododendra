package com.rhododendra.model;

public class ResolvedPhotoDetails {
    public String resolvedNormalURL;
    public String resolvedHiResURL;
    public String resolvedTagURL;
    public PhotoDetails photoDetails;

    public ResolvedPhotoDetails(
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
