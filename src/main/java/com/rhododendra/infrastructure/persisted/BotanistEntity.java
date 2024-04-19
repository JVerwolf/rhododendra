package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "HYBRIDIZERS",
    indexes = {
        @Index(columnList = "oldId", unique = true)
    }
)
public class BotanistEntity {
    @Id
    @GeneratedValue
    Long id;
    @Column(nullable = false) // Don't set unique here, apparently can clash with @Index above and create two indices.
    public String oldId; // The id before introducing a database with automatic generation

    @Column(unique = true)
    public String botanicalShorthand;
    public String location;
    public String bornDied;
    public String fullName;

    @OneToMany
    public List<PhotoDetailsEntity> photos;

    public BotanistEntity() {
    }

    public BotanistEntity(
        String oldId,
        String botanicalShorthand,
        String location,
        String bornDied,
        String fullName,
        List<PhotoDetailsEntity> photos
    ) {
        this.oldId = oldId;
        this.botanicalShorthand = botanicalShorthand;
        this.location = location;
        this.bornDied = bornDied;
        this.fullName = fullName;
        this.photos = photos;
    }
}
