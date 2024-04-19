package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

@Entity
@Table(name = "PHOTO_DETAILS",
    indexes = {
        @Index(columnList = "regularPhotoFileName", unique = true)
    }
)
public class PhotoDetailsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public String id;
    // Old ID
    @Column(nullable = false) // Don't set unique here, apparently can clash with @Index above and create two indices.
    public String regularPhotoFileName;
    public String hiResPhotoFileName;

    public String title; // Name field. Short description (e.g. rhododendron or person name)
    public String description; // Detailed description.
    public String photoBy;
    public String date; // Should be date taken.
    public String location;
    public Licence licence = Licence.COPYRIGHT;

    public enum Licence {
        CC_BY_40,
        CC_BY_SA_40,
        COPYRIGHT
    }

    public PhotoDetailsEntity() {
    }

    public PhotoDetailsEntity(
        String regularPhotoFileName,
        String hiResPhotoFileName,
        String title,
        String description,
        String photoBy,
        String date,
        String location,
        Licence licence
    ) {
        this.regularPhotoFileName = regularPhotoFileName;
        this.hiResPhotoFileName = hiResPhotoFileName;
        this.title = title;
        this.description = description;
        this.photoBy = photoBy;
        this.date = date;
        this.location = location;
        this.licence = licence;
    }
}
