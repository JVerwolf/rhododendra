package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class BotanicalSynonymEntity {
    @Id
    @GeneratedValue
    private long id;

    public String synonym;

    @ElementCollection
    @CollectionTable(name = "BOTANIST_SHORTHAND_TO_SYNONYM", joinColumns = @JoinColumn(name = "synonymId"))
    public List<String> botanistShorthand;

    public BotanicalSynonymEntity() {
    }

    public BotanicalSynonymEntity(String synonym, List<String> botanistShorthand) {
        this.synonym = synonym;
        this.botanistShorthand = botanistShorthand;
    }
}
