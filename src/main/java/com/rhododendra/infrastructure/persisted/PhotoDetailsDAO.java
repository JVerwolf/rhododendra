package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

@Entity
@Table(name = "PHOTO_DETAILS",
    indexes = {
        @Index(columnList = "photoFileName", unique = true)
    }
)
public class PhotoDetailsDAO {
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
    public Licence licence;

    public enum Licence {
        CC_BY_40,
        CC_BY_SA_40,
        COPYWRITE
    }
}
