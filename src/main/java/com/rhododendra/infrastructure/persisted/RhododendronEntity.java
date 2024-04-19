package com.rhododendra.infrastructure.persisted;

import com.rhododendra.model.Rhododendron;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(
    name = "RHODODENDRONS",
    indexes = {
        @Index(columnList = "oldId", unique = true)
    }
)
public class RhododendronEntity {
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
    public List<String> commonSynonyms;
    public String hardiness; // TODO needs to be a number
    public String deciduous;
    public String colour;  // TODO needs to be more granular.
    public String extraInformation;
    public Rhododendron.Lepedote lepedote;
    public Rhododendron.RhodoCategory rhodoCategory;
    public Rhododendron.SpeciesOrCultivar speciesOrCultivar;

    public String azaleaGroup = null; // Azaleas
    @Embedded
    public Parentage parentage; // Hybrids and Natural Hybrids
    @Embedded
    public HybridizationInfo hybridizerInfo; // Selections and Hybrids
    @Embedded
    public BotanicalInfo botanicalInfo; // Species and Natural Hybrids

    public RhododendronEntity(
        String oldId,
        String name,
        String tenYearHeight,
        String bloomTime,
        String flowerShape,
        String leafShape,
        List<String> commonSynonyms,
        String hardiness,
        String deciduous,
        String colour,
        String extraInformation,
        Rhododendron.Lepedote lepedote,
        Rhododendron.RhodoCategory rhodoCategory,
        Rhododendron.SpeciesOrCultivar speciesOrCultivar,
        String azaleaGroup,
        Parentage parentage,
        HybridizationInfo hybridizerInfo,
        BotanicalInfo botanicalInfo,
        List<RhodoPhotoEntity> photoDetails
    ) {
        this.oldId = oldId;
        this.name = name;
        this.tenYearHeight = tenYearHeight;
        this.bloomTime = bloomTime;
        this.flowerShape = flowerShape;
        this.leafShape = leafShape;
        this.commonSynonyms = commonSynonyms;
        this.hardiness = hardiness;
        this.deciduous = deciduous;
        this.colour = colour;
        this.extraInformation = extraInformation;
        this.lepedote = lepedote;
        this.rhodoCategory = rhodoCategory;
        this.speciesOrCultivar = speciesOrCultivar;
        this.azaleaGroup = azaleaGroup;
        this.parentage = parentage;
        this.hybridizerInfo = hybridizerInfo;
        this.botanicalInfo = botanicalInfo;
        this.photoDetails = photoDetails;
    }

    @OneToMany
    @JoinTable(name = "RHODODENDRONS_TO_PHOTOS",
        joinColumns = {@JoinColumn(name = "Photo_ID1")},
        inverseJoinColumns = {@JoinColumn(name = "Photo_ID2")}) // TODO inspect what this creates
    public List<RhodoPhotoEntity> photoDetails;


//    public enum SpeciesOrCultivar {
//        CULTIVAR,
//        SPECIES
//    }

//    public enum RhodoCategory {
//        AZALEODENDRON,
//        AZALEA,
//        RHODO,
//        VIREYA,
//        UNKNOWN
//    }

//    public enum Lepedote {
//        LEPEDOTE,
//        ELEPEDOTE,
//    }

    /**
     * This class stores data about plants of wild origin: Species and Natural Hybrids. Selections' parents  are
     * referenced from the HybridizerInfo class.
     */
    @Embeddable
    @Table(name = "BOTANICAL_INFO")
    public static class BotanicalInfo {
        public boolean isNaturalHybrid;

        public Taxonomy taxonomy;
        public String firstDescribed;
        @OneToMany
        public List<BotanistEntity> firstDescribedBotanists;
        public String originLocation;
        public String habit; // terrestrial or epyphytic
        public String observedMatureHeight;
        @OneToMany
        public List<BotanicalSynonymEntity> botanicalSynonyms;

        public BotanicalInfo() {
        }

        public BotanicalInfo(
            boolean isNaturalHybrid,
            Taxonomy taxonomy,
            String firstDescribed,
            List<BotanistEntity> firstDescribedBotanists,
            String originLocation,
            String habit,
            String observedMatureHeight,
            List<BotanicalSynonymEntity> botanicalSynonyms
        ) {
            this.isNaturalHybrid = isNaturalHybrid;
            this.taxonomy = taxonomy;
            this.firstDescribed = firstDescribed;
            this.firstDescribedBotanists = firstDescribedBotanists;
            this.originLocation = originLocation;
            this.habit = habit;
            this.observedMatureHeight = observedMatureHeight;
            this.botanicalSynonyms = botanicalSynonyms;
        }

        @Embeddable
        public static class Taxonomy {
            public String subgenus;
            public String section;
            public String subsection;

            public Taxonomy() {
            }

            public Taxonomy(String subgenus, String section, String subsection) {
                this.subgenus = subgenus;
                this.section = section;
                this.subsection = subsection;
            }
        }
    }


    @Embeddable
    public static class HybridizationInfo {
        public String HybridizationDetails; // TODO needs the date extracted to the yearDeveloped field.

        public String irrcRegistered;
        public String additionalParentageInfo;

        @ManyToOne
        public RhododendronEntity selectionSpecies; // for selection

        public String cultivationSince;
        public int yearDeveloped; // TODO make a date? could also do in the model translation logic.

        public boolean isSpeciesSelection;
        public boolean isCultivarGroup;

        @ManyToOne
        public HybridizerEntity hybridizer;

        public HybridizationInfo() {
        }

        public HybridizationInfo(
            String hybridizationDetails,
            String irrcRegistered,
            String additionalParentageInfo,
            RhododendronEntity selectionSpecies,
            String cultivationSince,
            int yearDeveloped,
            boolean isSpeciesSelection,
            boolean isCultivarGroup,
            HybridizerEntity hybridizer
        ) {
            HybridizationDetails = hybridizationDetails;
            this.irrcRegistered = irrcRegistered;
            this.additionalParentageInfo = additionalParentageInfo;
            this.selectionSpecies = selectionSpecies;
            this.cultivationSince = cultivationSince;
            this.yearDeveloped = yearDeveloped;
            this.isSpeciesSelection = isSpeciesSelection;
            this.isCultivarGroup = isCultivarGroup;
            this.hybridizer = hybridizer;
        }
    }


    @Embeddable
    public static class Parentage {
        // some entries don't link they just have text
        // TODO remove this once links have been properly established.
        public String seedParentFallbackDescription;
        @ManyToOne(fetch = FetchType.LAZY)
        public RhododendronEntity seedParent;

        // some entries don't link they just have text
        // TODO remove this once links have been properly established.
        public String pollenParentFallbackDescription;
        @ManyToOne(fetch = FetchType.LAZY)
        public RhododendronEntity pollenParent;

        public Parentage() {
        }

        public Parentage(
            String seedParentFallbackDescription,
            RhododendronEntity seedParent,
            String pollenParentFallbackDescription,
            RhododendronEntity pollenParent
        ) {
            this.seedParentFallbackDescription = seedParentFallbackDescription;
            this.seedParent = seedParent;
            this.pollenParentFallbackDescription = pollenParentFallbackDescription;
            this.pollenParent = pollenParent;
        }
    }
}
