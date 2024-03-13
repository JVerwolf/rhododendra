package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

@Entity
@Table(name = "PHOTO_DETAILS",
    indexes = {
        @Index(columnList = "photoFileName", unique = true)
    }
)
public class RhododendronPhotoDAO {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    public String tagPhotoFileName;

    @ManyToOne
    public RhododendronDAO rhododendron;

    @OneToOne
    public PhotoDetailsDAO photoDetails;
}
