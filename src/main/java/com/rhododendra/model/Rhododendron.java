package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rhododendra.service.RhodoLogicService;
import jakarta.persistence.*;

import java.util.List;

//@JsonIgnoreProperties(ignoreUnknown=true)
@Entity
@Table(name = "RHODODENDRONS")
@JsonIgnoreProperties({"path"})
public class Rhododendron extends Indexable {
    public static final String PRIMARY_ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String NAME_KEY_FOR_SORT = "name_key_for_sort";
    public static final String SEARCH_FILTERS = "search_filters";
    @Id
    String id;
    String name;
    String ten_year_height;
    String bloom_time;
    String flower_shape;
    String leaf_shape;
    //    @OneToMany
//    @JoinTable(name="RHODODENDRONS_TO_PHOTOS",
//            joinColumns={@JoinColumn(name="Photo_ID")},
//        inverseJoinColumns={@JoinColumn(name="Photo_ID")})
    @ElementCollection
    @CollectionTable(name = "RHODODENDRONS_TO_PHOTOS", joinColumns = @JoinColumn(name = "RhododendronId"))
    @Column(name = "PhotoId")
    List<String> photos; // todo these are FKs
    @ElementCollection
    List<String> synonyms;
    String hardiness;
    String deciduous;
    String colour;  // TODO needs to be more granular.
    String extra_information;

    // Selections and Hybrids

    Hybridizer hybridizer;
    String irrc_registered;
    String additional_parentage_info;
    String species_id; // for selection
    @Transient
    Rhododendron selectedSpecies; // Don't store as this info can change, not the source of truth. Only for fetching and putting in model for display.
    String cultivation_since;
    Lepedote lepedote;

    // Hybrids and Natural Hybrids
    Parentage parentage;

    // Species
    Taxonomy taxonomy;
    String first_described;
    @ElementCollection
    List<String> first_described_botanists;
    String origin_location;
    String habit; // terestrial or epyphytic
    String observed_mature_height;
    @ElementCollection
    List<Synonym> botanical_synonyms;

    // Azaleas
    @JsonProperty("azalea_group")
    String azalea_group = null;

    SpeciesOrCultivar speciesOrCultivar;
    boolean is_species_selection;
    boolean is_natural_hybrid;
    boolean is_cultivar_group;
    RhodoCategory rhodoCategory;

    @JsonIgnore
    @Override
    public String primaryIdValue() {
        return id;
    }

    public static enum SpeciesOrCultivar {
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

    public enum SearchFilters {
        BOTANICAL,
        CULTIVAR
    }

    @JsonIgnore
    public SearchFilters getSearchFilter() {
        boolean isBotanical = isSpecies()
            || getIs_species_selection()
            || getIs_natural_hybrid();
        if (isBotanical) {
            return SearchFilters.BOTANICAL;
        } else {
            return SearchFilters.CULTIVAR;
        }
    }

    @JsonIgnore
    public String descriptionText() {
        var category = "";
        if (this.getRhodoCategory() == RhodoCategory.RHODO) {
            category = "Rhododendron";
        } else if (this.getRhodoCategory() == RhodoCategory.AZALEA) {
            category = "Azalea";
        } else if (this.getRhodoCategory() == RhodoCategory.VIREYA) {
            category = "Vireya";
        } else if (this.getRhodoCategory() == RhodoCategory.AZALEODENDRON) {
            category = "Azaleodendron";
        }
        if (is_cultivar_group) {
            if (this.getRhodoCategory() == RhodoCategory.UNKNOWN) {
                return "Cultivar group";
                // TODO logger.warn, shouldn't have an unknown cultivar group.
            }
            return category + " Cultivar group";
        } else if (is_species_selection) {
            if (this.getRhodoCategory() == RhodoCategory.UNKNOWN) {
                return "Species Selection";
                // TODO logger.warn, shouldn't have an unknown species selection.
            }
            return category + " Species Selection";
        } else if (is_natural_hybrid) { // TODO future there can be selections of natural hybrids.
            if (this.getRhodoCategory() == RhodoCategory.UNKNOWN) {
                return "Natural Hybrid";
            }
            return "Natural " + category + " Hybrid";
        } else if (isSpecies()) {
            if (this.getRhodoCategory() == RhodoCategory.UNKNOWN) {
                return "Species";
            }
            return category + " Species";
        } else {
            if (!isSpecies()) {
                if (this.getRhodoCategory() == RhodoCategory.UNKNOWN) {
                    return "Hybrid";
                    // TODO logger.warn, shouldn't have an unknown species selection.
                }
                return category + " Hybrid";
            }
            return ""; // TODO logger error.
        }
    }

    @JsonIgnore
    public String getSeedParentId() {
        if (parentage != null && parentage.seed_parent_id != null) {
            return parentage.seed_parent_id;
        } else if (is_species_selection) {
            return getSpecies_id();
        } else if (isSpecies() && !is_natural_hybrid) {
            return this.id;
        }
        return null;
    }

    @JsonIgnore
    public String getPollenParentId() {
        if (parentage != null && parentage.pollen_parent_id != null) {
            return parentage.pollen_parent_id;
        } else if (is_species_selection) {
            return getSpecies_id();
        } else if (isSpecies() && !is_natural_hybrid) {
            return this.id;
        }
        return null;
    }

    @JsonIgnore
    public boolean isSpecies() {
        return this.getSpeciesOrCultivar() == SpeciesOrCultivar.SPECIES;
    }

    @JsonIgnore
    public boolean isCultivar() {
        return this.getSpeciesOrCultivar() == SpeciesOrCultivar.CULTIVAR;
    }

    // TODO Bad, this is a temporary hack. This class should not depend on anything else, as it's in the model.
    @JsonIgnore
    public String formatSynonymName(String speciesName) {
        return RhodoLogicService.formatSynonymName(speciesName);
    }

    @JsonIgnore
    public String getFormattedName() {
        return RhodoLogicService.getFormattedRhodoName(this);
    }

    // TODO Bad, this is a temporary hack. This class should not depend on anything else, as it's in the model.
    @JsonIgnore
    public String getFormattedSeedParentName() {
        return RhodoLogicService.getFormattedSeedParentName(this);
    }

    // TODO Bad, this is a temporary hack. This class should not depend on anything else, as it's in the model.
    @JsonIgnore
    public String getFormattedPollenParentName() {
        return RhodoLogicService.getFormattedPollenParentName(this);
    }

    @JsonIgnore
    public boolean isHybridRhodo() {
        return this.getRhodoCategory() == RhodoCategory.RHODO
            && !this.isSpecies();
    }

    @JsonIgnore
    public boolean isCultivarGroup() {
        return this.getRhodoCategory() == RhodoCategory.RHODO // TODO will need to check for other categories at usage sites if this is removed.
            && this.getIs_cultivar_group();
    }

    @JsonIgnore
    public boolean isAzaleaHybrid() {
        return this.getRhodoCategory() == RhodoCategory.AZALEA
            && !this.isSpecies();
    }

    @JsonIgnore
    public boolean isVireyaHybrid() {
        return this.getRhodoCategory() == RhodoCategory.VIREYA
            && !this.isSpecies();
    }

    @JsonIgnore
    public boolean isAzaleodendronHybrid() {
        return this.getRhodoCategory() == RhodoCategory.AZALEODENDRON
            && !this.isSpecies();
    }


    public enum Lepedote {
        LEPEDOTE,
        ELEPEDOTE,
    }

    @JsonIgnore
    public boolean isLepedote() {
        return this.lepedote == Lepedote.LEPEDOTE;
    }

    @JsonIgnore
    public boolean isElepedote() {
        return this.lepedote == Lepedote.ELEPEDOTE;
    }

    @Embeddable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Synonym(
        @JsonProperty(SYNONYM_KEY) String synonym,
        @JsonProperty(BOTANICAL_SHORTS_KEY) List<String> botanical_shorts
    ) {
        public static final String SYNONYM_KEY = "synonym";
        public static final String BOTANICAL_SHORTS_KEY = "botanical_shorts";
    }

    @Embeddable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hybridizer {
        String hybridizer;
        String hybridizer_id;

        public String getHybridizer() {
            return hybridizer;
        }

        public void setHybridizer(String hybridizer) {
            this.hybridizer = hybridizer;
        }

        // TODO add JPA annotation to reference foreign key
        @CollectionTable(name = "RHODODENDRONS_TO_HYBRIDIZERS", joinColumns = @JoinColumn(name = "RhododendronId"))
        @Column(name = "hybridizerId")
        public String getHybridizer_id() {
            return hybridizer_id;
        }

        public void setHybridizer_id(String hybridizer_id) {
            this.hybridizer_id = hybridizer_id;
        }
    }

    @Embeddable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Taxonomy {
        String subgenus;
        String section;
        String subsection;

        public String getSubgenus() {
            return subgenus;
        }

        public void setSubgenus(String subgenus) {
            this.subgenus = subgenus;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getSubsection() {
            return subsection;
        }

        public void setSubsection(String subsection) {
            this.subsection = subsection;
        }
    }

    @Embeddable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parentage {
        String seed_parent;
        String seed_parent_id;
        String pollen_parent;
        String pollen_parent_id;

        public String getSeed_parent() {
            return seed_parent;
        }

        public void setSeed_parent(String seed_parent) {
            this.seed_parent = seed_parent;
        }

        public String getSeed_parent_id() {
            return seed_parent_id;
        }

        public void setSeed_parent_id(String seed_parent_id) {
            this.seed_parent_id = seed_parent_id;
        }

        public String getPollen_parent() {
            return pollen_parent;
        }

        public void setPollen_parent(String pollen_parent) {
            this.pollen_parent = pollen_parent;
        }

        public String getPollen_parent_id() {
            return pollen_parent_id;
        }

        public void setPollen_parent_id(String pollen_parent_id) {
            this.pollen_parent_id = pollen_parent_id;
        }
    }

    public SpeciesOrCultivar getSpeciesOrCultivar() {
        return speciesOrCultivar;
    }

    public void setSpeciesOrCultivar(SpeciesOrCultivar speciesOrCultivar) {
        this.speciesOrCultivar = speciesOrCultivar;
    }

    public boolean getIs_species_selection() {
        return is_species_selection;
    }

    public void setIs_species_selection(boolean is_species_selection) {
        this.is_species_selection = is_species_selection;
    }

    public RhodoCategory getRhodoCategory() {
        return rhodoCategory;
    }

    public void setRhodoCategory(RhodoCategory rhodoCategory) {
        this.rhodoCategory = rhodoCategory;
    }

    public boolean getIs_natural_hybrid() {
        return is_natural_hybrid;
    }

    public void setIs_natural_hybrid(boolean is_natural_hybrid) {
        this.is_natural_hybrid = is_natural_hybrid;
    }

    public boolean getIs_cultivar_group() {
        return is_cultivar_group;
    }

    public void setIs_cultivar_group(boolean is_cultivar_group) {
        this.is_cultivar_group = is_cultivar_group;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTen_year_height() {
        return ten_year_height;
    }

    public void setTen_year_height(String ten_year_height) {
        this.ten_year_height = ten_year_height;
    }

    public String getBloom_time() {
        return bloom_time;
    }

    public void setBloom_time(String bloom_time) {
        this.bloom_time = bloom_time;
    }

    public String getFlower_shape() {
        return flower_shape;
    }

    public void setFlower_shape(String flower_shape) {
        this.flower_shape = flower_shape;
    }

    public String getLeaf_shape() {
        return leaf_shape;
    }

    public void setLeaf_shape(String leaf_shape) {
        this.leaf_shape = leaf_shape;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public String getHardiness() {
        return hardiness;
    }

    public void setHardiness(String hardiness) {
        this.hardiness = hardiness;
    }

    public String getDeciduous() {
        return deciduous;
    }

    public void setDeciduous(String deciduous) {
        this.deciduous = deciduous;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getExtra_information() {
        return extra_information;
    }

    public void setExtra_information(String extra_information) {
        this.extra_information = extra_information;
    }

    public Hybridizer getHybridizer() {
        return hybridizer;
    }

    public void setHybridizer(Hybridizer hybridizer) {
        this.hybridizer = hybridizer;
    }

    public String getIrrc_registered() {
        return irrc_registered;
    }

    public void setIrrc_registered(String irrc_registered) {
        this.irrc_registered = irrc_registered;
    }

    public String getAdditional_parentage_info() {
        return additional_parentage_info;
    }

    public void setAdditional_parentage_info(String additional_parentage_info) {
        this.additional_parentage_info = additional_parentage_info;
    }

    public String getSpecies_id() {
        return species_id;
    }

    public void setSpecies_id(String species_id) {
        this.species_id = species_id;
    }

    @JsonIgnore
    public Rhododendron getSelectedSpecies() {
        return selectedSpecies;
    }

    @JsonIgnore
    public void setSelectedSpecies(Rhododendron selectedSpecies) {
        this.selectedSpecies = selectedSpecies;
    }

    public String getCultivation_since() {
        return cultivation_since;
    }

    public void setCultivation_since(String cultivation_since) {
        this.cultivation_since = cultivation_since;
    }

    public Lepedote getLepedote() {
        return lepedote;
    }

    public void setLepedote(Lepedote lepedote) {
        this.lepedote = lepedote;
    }

    public Parentage getParentage() {
        return parentage;
    }

    public void setParentage(Parentage parentage) {
        this.parentage = parentage;
    }

    public Taxonomy getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    public String getFirst_described() {
        return first_described;
    }

    public void setFirst_described(String first_described) {
        this.first_described = first_described;
    }

    public List<String> getFirst_described_botanists() {
        return first_described_botanists;
    }

    public void setFirst_described_botanists(List<String> first_described_botanists) {
        this.first_described_botanists = first_described_botanists;
    }

    public String getOrigin_location() {
        return origin_location;
    }

    public void setOrigin_location(String origin_location) {
        this.origin_location = origin_location;
    }

    public String getHabit() {
        return habit;
    }

    public void setHabit(String habit) {
        this.habit = habit;
    }

    public String getObserved_mature_height() {
        return observed_mature_height;
    }

    public void setObserved_mature_height(String observed_mature_height) {
        this.observed_mature_height = observed_mature_height;
    }

    public List<Synonym> getBotanical_synonyms() {
        return botanical_synonyms;
    }

    public void setBotanical_synonyms(List<Synonym> botanical_synonyms) {
        this.botanical_synonyms = botanical_synonyms;
    }

    public String getAzalea_group() {
        return azalea_group;
    }

    public void setAzalea_group(String azalea_group) {
        this.azalea_group = azalea_group;
    }
}
