package com.rhododendra.infrastructure.persisted;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(
    name = "RHODODENDRONS",
    indexes = {
        @Index(columnList = "oldId", unique = true)
    }
)
public class RhododendronDAO {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;
    @Column(nullable = false) // Don't set unique here, apparently can clash with @Index above and create two indices.
    public String oldId; // The id before introducing a database with automatic generation

    public String name;
    public String tenYearHeight;
    public String bloomTime; // Use ARS style for reference
    public String flowerShape;
    public String leafShape;
    @ElementCollection
    public List<String> common_synonyms;
    public String hardiness; // TODO needs to be a number
    public String deciduous;
    public String colour;  // TODO needs to be more granular.
    public String extraInformation;
    public Lepedote lepedote;
    public RhodoCategory rhodoCategory;
    public SpeciesOrCultivar speciesOrCultivar;

    public String azaleaGroup = null; // Azaleas
    @Embedded
    public Parentage parentage; // Hybrids and Natural Hybrids
    @Embedded
    public HybridizationInfo hybridizerInfo; // Selections and Hybrids
    @Embedded
    public BotanicalInfoDAO speciesInfo; // Species and Natural Hybrids


    @OneToMany
    @JoinTable(name = "RHODODENDRONS_TO_PHOTOS",
        joinColumns = {@JoinColumn(name = "Photo_ID1")},
        inverseJoinColumns = {@JoinColumn(name = "Photo_ID2")}) // TODO inspect what this creates
    public List<RhododendronPhotoDAO> photoDetails;


    public enum SpeciesOrCultivar {
        CULTIVAR,
        SPECIES
    }

    public enum RhodoCategory {
        AZALEODENDRON,
        AZALEA,
        RHODO,
        VIREYA,
        UNKNOWN
    }

    public enum Lepedote {
        LEPEDOTE,
        ELEPEDOTE,
    }

    @Embeddable
    @Table(name = "BOTANICAL_INFO")
    public static class BotanicalInfoDAO {
        public boolean isSpeciesSelection;
        public boolean isNaturalHybrid;

        public Taxonomy taxonomy;
        public String firstDescribed;
        @OneToMany
        public List<BotanistDAO> firstDescribedBotanists;
        public String originLocation;
        public String habit; // terrestrial or epyphytic
        public String observedMatureHeight;
        @OneToMany
        public List<BotanicalSynonymDAO> botanicalSynonyms;

        @Embeddable
        public static class Taxonomy {
            String subgenus;
            String section;
            String subsection;
        }
    }


    @Entity
    public static class BotanicalSynonymDAO {
        @Id
        @GeneratedValue
        private long id;

        public String synonym;

        @ElementCollection
        @CollectionTable(name = "BOTANIST_SHORTHAND_TO_SYNONYM", joinColumns = @JoinColumn(name = "synonymId"))
        public List<String> botanistShorthand;
    }


    @Embeddable
    public static class HybridizationInfo {
        public String HybridizationDetails; // TODO needs the date extracted

        public String irrcRegistered;
        public String additionalParentageInfo;
        public String speciesId; // for selection
        public String cultivationSince;

        public int year; // TODO make a date? could also do in the model translation logic.

        public boolean isCultivarGroup;

        @ManyToOne
        public HybridizerDAO hybridizer;
    }


    @Embeddable
    public static class Parentage {
        public String seedParentFallbackDescription; // some entries don't link they just have text
        @ManyToOne(fetch = FetchType.LAZY)
        public RhododendronDAO seedParent;

        public String pollenParentFallbackDescription;  // some entries don't link they just have text
        @ManyToOne(fetch = FetchType.LAZY)
        public RhododendronDAO pollenParent;
    }
}
