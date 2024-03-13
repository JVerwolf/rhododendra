package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "HYBRIDIZERS",
    indexes = {
        @Index(columnList = "oldId", unique = true)
    }
)
public class HybridizerDAO {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;
    @Column(nullable = false) // Don't set unique here, apparently can clash with @Index above and create two indices.
    public String oldId; // The id before introducing a database with automatic generation

    public String name;
    public String location;

    @OneToMany
    public List<PhotoDetailsDAO> photos;
}
