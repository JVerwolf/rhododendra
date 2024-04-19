package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

@Entity
@Table(name = "RHODO_PHOTO_DETAILS",
    indexes = {
        @Index(columnList = "regularPhotoFileName", unique = true)
    }
)
public class RhodoPhotoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    // alt ID
    @Column(nullable = false) // Don't set unique here, apparently can clash with @Index above and create two indices.
    public String regularPhotoFileName;

    public String tagPhotoFileName;

    @ManyToOne
    public RhododendronEntity rhododendron;

    @OneToOne
    public PhotoDetailsEntity photoDetails;

    public RhodoPhotoEntity() {
    }

    public RhodoPhotoEntity(
        String tagPhotoFileName,
        RhododendronEntity rhododendron,
        PhotoDetailsEntity photoDetails
    ) {
        this.tagPhotoFileName = tagPhotoFileName;
        this.rhododendron = rhododendron;
        this.photoDetails = photoDetails;
        this.regularPhotoFileName = photoDetails.regularPhotoFileName;
    }
}
