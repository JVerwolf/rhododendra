package com.rhododendra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Hybrid extends Indexable {
    public static final String  PRIMARY_ID_KEY = "id";
    public static final String  NAME_KEY = "name";
    public static final String NAME_KEY_FOR_SORT = "name_key_for_sort";

    String id;
    String name;
    boolean is_cultivar_group;
    String hybridizer;
    String hybridizer_link;
    Integer hybridizer_id;
    @JsonProperty("10_year_height")
    String ten_year_height;
    String bloom_time;
    String flower;
    String leaves;
    List<String> photos;
    String additional_parentage_info;
    boolean is_species_selection;
    String status;
    List<String> synonymns;
    String type;
    String hardiness;
    Genetics genetics;
    String species_id;

    public String getSpecies_id() {
        return species_id;
    }

    public void setSpecies_id(String species_id) {
        this.species_id = species_id;
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

    public boolean getIs_cultivar_group() {
        return is_cultivar_group;
    }

    public void setIs_cultivar_group(boolean is_cultivar_group) {
        this.is_cultivar_group = is_cultivar_group;
    }

    public String getHybridizer() {
        return hybridizer;
    }

    public void setHybridizer(String hybridizer) {
        this.hybridizer = hybridizer;
    }

    public String getHybridizer_link() {
        return hybridizer_link;
    }

    public void setHybridizer_link(String hybridizer_link) {
        this.hybridizer_link = hybridizer_link;
    }

    public Integer getHybridizer_id() {
        return hybridizer_id;
    }

    public void setHybridizer_id(Integer hybridizer_id) {
        this.hybridizer_id = hybridizer_id;
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

    public String getFlower() {
        return flower;
    }

    public void setFlower(String flower) {
        this.flower = flower;
    }

    public String getLeaves() {
        return leaves;
    }

    public void setLeaves(String leaves) {
        this.leaves = leaves;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public String getAdditional_parentage_info() {
        return additional_parentage_info;
    }

    public void setAdditional_parentage_info(String additional_parentage_info) {
        this.additional_parentage_info = additional_parentage_info;
    }

    public boolean getIs_species_selection() {
        return is_species_selection;
    }

    public void setIs_species_selection(boolean is_species_selection) {
        this.is_species_selection = is_species_selection;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSynonymns() {
        return synonymns;
    }

    public void setSynonymns(List<String> synonymns) {
        this.synonymns = synonymns;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHardiness() {
        return hardiness;
    }

    public void setHardiness(String hardiness) {
        this.hardiness = hardiness;
    }

    public Genetics getGenetics() {
        return genetics;
    }

    public void setGenetics(Genetics genetics) {
        this.genetics = genetics;
    }

    @JsonIgnore
    @Override
    public String primaryIdValue() {
        return getId();
    }
}
