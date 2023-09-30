package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Species(
    @JsonProperty(ID_KEY) String id,
    @JsonProperty(NAME_KEY) String name,
    @JsonProperty(SUBGENUS_KEY) String subgenus,
    @JsonProperty(SECTION_KEY) String section,
    @JsonProperty(SUBSECTION_KEY) String subsection,
    @JsonProperty(IS_NATURAL_HYBRID_KEY) Boolean is_natural_hybrid,
    @JsonProperty(SEED_PARENT_KEY) String seed_parent,
    @JsonProperty(POLLENT_PARENT_KEY) String pollen_parent,
    @JsonProperty(CULTIVATION_SINCE_KEY) String cultivation_since,
    @JsonProperty(FIRST_DESCRIBED_KEY) String first_described,
    @JsonProperty(FIRST_DESCRIBED_BOTANISTS_KEY) List<String> first_described_botanists,
    @JsonProperty(ORIGIN_KEY) String origin,
    @JsonProperty(COROLLA_KEY) String corolla,
    @JsonProperty(LEAVES_KEY) String leaves,
    @JsonProperty(HABIT_KEY) String habit,
    @JsonProperty(WINTER_KEY) String winter,
    @JsonProperty(BLOOM_TIME_KEY) String bloom_time,
    @JsonProperty(HARDINESS_KEY) String hardiness,
    @JsonProperty(PREDOMINANT_COLOUR_KEY) String predominant_colour,
    @JsonProperty(HEIGHT_KEY) String height,
    @JsonProperty(COMMON_NAMES_KEY) List<String> common_names,
    @JsonProperty(SYNONYMS_KEY) List<Synonym> synonyms,
    @JsonProperty(PHOTOS_KEY) List<String> photos,
    @JsonProperty(EXTRA_INFORMATION_KEY) String extra_information
) implements PrimaryID {
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

    @JsonIgnore
    @Override
    public String primaryIdValue() {
        return this.id;
    }
}
