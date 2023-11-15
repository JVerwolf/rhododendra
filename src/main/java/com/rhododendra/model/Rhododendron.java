package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

//@JsonIgnoreProperties(ignoreUnknown=true)
@JsonIgnoreProperties({"path"})
public class Rhododendron extends Indexable {
    public static final String PRIMARY_ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String NAME_KEY_FOR_SORT = "name_key_for_sort";
    public static final String RHODO_DATA_TYPE = "rhodoDataType";

    String id;
    String name;
    RhodoDataType rhodoDataType; // TODO needs to be snake-case
    String ten_year_height;
    String bloom_time;
    String flower_shape;
    String leaf_shape;
    List<String> photos;
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
    String cultivation_since;
    Lepedote lepedote;

    // Hybrids and Natural Hybrids
    Parentage parentage;

    // Species
    Taxonomy taxonomy;
    String first_described;
    List<String> first_described_botanists;
    String origin_location;
    String habit; // terestrial or epyphytic
    String observed_mature_height;
    List<Synonym> botanical_synonyms;

    // Azaleas
    @JsonProperty("azalea_group")
    String azalea_group = null;

    @JsonIgnore
    @Override
    public String primaryIdValue() {
        return id;
    }

    public static enum RhodoDataType {
        SPECIES,
        SPECIES_SELECTION,
        NATURAL_HYBRID,
        RHODO_HYBRID,
        CULTIVAR_GROUP,
        AZALEA_HYBRID,
        VIREYA_HYBRID,
        AZALEODENDRON_HYBRID
    }

    @JsonIgnore
    public boolean isSpecies() {
        return this.getRhodoDataType() == RhodoDataType.SPECIES;
    }

    @JsonIgnore
    public boolean isSpeciesSelection() {
        return this.getRhodoDataType() == RhodoDataType.SPECIES_SELECTION;
    }

    @JsonIgnore
    public boolean isNaturalHybrid() {
        return this.getRhodoDataType() == RhodoDataType.NATURAL_HYBRID;
    }

    @JsonIgnore
    public boolean isCultivar() {
        return this.getRhodoDataType() == RhodoDataType.RHODO_HYBRID
            || this.getRhodoDataType() == RhodoDataType.AZALEA_HYBRID
            || this.getRhodoDataType() == RhodoDataType.VIREYA_HYBRID
            || this.getRhodoDataType() == RhodoDataType.AZALEODENDRON_HYBRID;
    }

    @JsonIgnore
    public boolean isHybridRhodo() {
        return this.getRhodoDataType() == RhodoDataType.RHODO_HYBRID;
    }

    @JsonIgnore
    public boolean isCultivarGroup() {
        return this.getRhodoDataType() == RhodoDataType.CULTIVAR_GROUP;
    }

    @JsonIgnore
    public boolean isAzaleaHybrid() {
        return this.getRhodoDataType() == RhodoDataType.AZALEA_HYBRID;
    }

    @JsonIgnore
    public boolean isVireyaHybrid() {
        return this.getRhodoDataType() == RhodoDataType.VIREYA_HYBRID;
    }

    @JsonIgnore
    public boolean isAzaleodendronHybrid() {
        return this.getRhodoDataType() == RhodoDataType.AZALEODENDRON_HYBRID;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Synonym(
        @JsonProperty(SYNONYM_KEY) String synonym,
        @JsonProperty(BOTANICAL_SHORTS_KEY) List<String> botanical_shorts
    ) {
        public static final String SYNONYM_KEY = "synonym";
        public static final String BOTANICAL_SHORTS_KEY = "botanical_shorts";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hybridizer {
        String hybridizer;
        Integer hybridizer_id;

        public String getHybridizer() {
            return hybridizer;
        }

        public void setHybridizer(String hybridizer) {
            this.hybridizer = hybridizer;
        }

        public Integer getHybridizer_id() {
            return hybridizer_id;
        }

        public void setHybridizer_id(Integer hybridizer_id) {
            this.hybridizer_id = hybridizer_id;
        }
    }

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

    public RhodoDataType getRhodoDataType() {
        return rhodoDataType;
    }

    public void setRhodoDataType(RhodoDataType rhodoDataType) {
        this.rhodoDataType = rhodoDataType;
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
