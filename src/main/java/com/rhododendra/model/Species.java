package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Species implements PrimaryID {
    public static final String ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String SUBGENUS_KEY = "subgenus";
    public static final String SECTION_KEY = "section";
    public static final String SUBSECTION_KEY = "subsection";
    public static final String IS_NATURAL_HYBRID_KEY = "is_natural_hybrid";
    public static final String SEED_PARENT_KEY = "seed_parent";
    public static final String POLLENT_PARENT_KEY = "pollent_parent";
    public static final String CULTIVATION_SINCE_KEY = "cultivation_since";
    public static final String FIRST_DESCRIBED_KEY = "first_described";
    public static final String FIRST_DESCRIBED_BOTANISTS_KEY = "first_described_botanists";
    public static final String ORIGIN_KEY = "origin";
    public static final String COROLLA_KEY = "corolla";
    public static final String LEAVES_KEY = "leaves";
    public static final String HABIT_KEY = "habit";
    public static final String WINTER_KEY = "winter";
    public static final String BLOOM_TIME_KEY = "bloom_time";
    public static final String HARDINESS_KEY = "hardiness";
    public static final String PREDOMINANT_COLOUR_KEY = "predominant_colour";
    public static final String HEIGHT_KEY = "height";
    public static final String COMMON_NAMES_KEY = "common_names";
    public static final String SYNONYMS_KEY = "synonyms";
    public static final String PHOTOS_KEY = "photos";
    public static final String EXTRA_INFORMATION_KEY = "extra_information";
    public static final String PRIMARY_ID_KEY = ID_KEY;

    @JsonProperty(ID_KEY)
    String id;
    @JsonProperty(NAME_KEY)
    String name;
    @JsonProperty(SUBGENUS_KEY)
    String subgenus;
    @JsonProperty(SECTION_KEY)
    String section;
    @JsonProperty(SUBSECTION_KEY)
    String subsection;
    @JsonProperty(IS_NATURAL_HYBRID_KEY)
    Boolean is_natural_hybrid;
    @JsonProperty(SEED_PARENT_KEY)
    String seed_parent;
    @JsonProperty(POLLENT_PARENT_KEY)
    String pollen_parent;
    @JsonProperty(CULTIVATION_SINCE_KEY)
    String cultivation_since;
    @JsonProperty(FIRST_DESCRIBED_KEY)
    String first_described;
    @JsonProperty(FIRST_DESCRIBED_BOTANISTS_KEY)
    List<String> first_described_botanists;
    @JsonProperty(ORIGIN_KEY)
    String origin;
    @JsonProperty(COROLLA_KEY)
    String corolla;
    @JsonProperty(LEAVES_KEY)
    String leaves;
    @JsonProperty(HABIT_KEY)
    String habit;
    @JsonProperty(WINTER_KEY)
    String winter;
    @JsonProperty(BLOOM_TIME_KEY)
    String bloom_time;
    @JsonProperty(HARDINESS_KEY)
    String hardiness;
    @JsonProperty(PREDOMINANT_COLOUR_KEY)
    String predominant_colour;
    @JsonProperty(HEIGHT_KEY)
    String height;
    @JsonProperty(COMMON_NAMES_KEY)
    List<String> common_names;
    @JsonProperty(SYNONYMS_KEY)
    List<Synonym> synonyms;
    @JsonProperty(PHOTOS_KEY)
    List<String> photos;
    @JsonProperty(EXTRA_INFORMATION_KEY)
    String extra_informatio;

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

    public Boolean getIs_natural_hybrid() {
        return is_natural_hybrid;
    }

    public void setIs_natural_hybrid(Boolean is_natural_hybrid) {
        this.is_natural_hybrid = is_natural_hybrid;
    }

    public String getSeed_parent() {
        return seed_parent;
    }

    public void setSeed_parent(String seed_parent) {
        this.seed_parent = seed_parent;
    }

    public String getPollen_parent() {
        return pollen_parent;
    }

    public void setPollen_parent(String pollen_parent) {
        this.pollen_parent = pollen_parent;
    }

    public String getCultivation_since() {
        return cultivation_since;
    }

    public void setCultivation_since(String cultivation_since) {
        this.cultivation_since = cultivation_since;
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getCorolla() {
        return corolla;
    }

    public void setCorolla(String corolla) {
        this.corolla = corolla;
    }

    public String getLeaves() {
        return leaves;
    }

    public void setLeaves(String leaves) {
        this.leaves = leaves;
    }

    public String getHabit() {
        return habit;
    }

    public void setHabit(String habit) {
        this.habit = habit;
    }

    public String getWinter() {
        return winter;
    }

    public void setWinter(String winter) {
        this.winter = winter;
    }

    public String getBloom_time() {
        return bloom_time;
    }

    public void setBloom_time(String bloom_time) {
        this.bloom_time = bloom_time;
    }

    public String getHardiness() {
        return hardiness;
    }

    public void setHardiness(String hardiness) {
        this.hardiness = hardiness;
    }

    public String getPredominant_colour() {
        return predominant_colour;
    }

    public void setPredominant_colour(String predominant_colour) {
        this.predominant_colour = predominant_colour;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public List<String> getCommon_names() {
        return common_names;
    }

    public void setCommon_names(List<String> common_names) {
        this.common_names = common_names;
    }

    public List<Synonym> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<Synonym> synonyms) {
        this.synonyms = synonyms;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public String getExtra_informatio() {
        return extra_informatio;
    }

    public void setExtra_informatio(String extra_informatio) {
        this.extra_informatio = extra_informatio;
    }

    @JsonIgnore
    @Override
    public String primaryIdValue() {
        return this.id;
    }
}
