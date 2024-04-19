package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "HYBRIDIZERS",
    indexes = {
        @Index(columnList = "oldId", unique = true)
    }
)
public class HybridizerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;
    @Column(nullable = false) // Don't set unique here, apparently can clash with @Index above and create two indices.
    public String oldId; // The id before introducing a database with automatic generation

    public String name;
    public String location;

    @OneToMany
    public List<PhotoDetailsEntity> photos;

    public HybridizerEntity() {
    }

    public HybridizerEntity(String oldId, String name, String location, List<PhotoDetailsEntity> photos) {
        this.oldId = oldId;
        this.name = name;
        this.location = location;
        this.photos = photos;
    }
}
