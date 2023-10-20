package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Synonym(
    @JsonProperty(SYNONYM_KEY) String synonym,
    @JsonProperty(BOTANICAL_SHORTS_KEY) List<String> botanical_shorts
) {
    public static final String SYNONYM_KEY = "synonym";
    public static final String BOTANICAL_SHORTS_KEY = "botanical_shorts";
}
