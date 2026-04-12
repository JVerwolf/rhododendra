/*
 * Rhododendra — Spring Boot web application for rhododendron data.
 * Copyright (C) 2026 Rhododendra contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.rhododendra.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Botanist extends Indexable {
    public static final String BOTANICAL_SHORT_KEY = "botanical_short";
    public static final String LOCATION_KEY = "location";
    public static final String BORN_DIED_KEY = "born_died";
    public static final String FULL_NAME_KEY = "full_name";
    public static final String IMAGE_KEY = "image";

    public static final String PRIMARY_ID_KEY = "id";

    @JsonIgnore
    Long id;

    @JsonProperty(BOTANICAL_SHORT_KEY)
    String botanicalShort;
    @JsonProperty(LOCATION_KEY)
    String location;
    @JsonProperty(BORN_DIED_KEY)
    String bornDied;
    @JsonProperty(FULL_NAME_KEY)
    String fullName;
    @JsonProperty(IMAGE_KEY)
    String image;

    public String getBotanicalShort() {
        return botanicalShort;
    }

    public void setBotanicalShort(String botanicalShort) {
        this.botanicalShort = botanicalShort;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBornDied() {
        return bornDied;
    }

    public void setBornDied(String bornDied) {
        this.bornDied = bornDied;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    @Override
    public String primaryIdValue() {
        return id == null ? "" : String.valueOf(id);
    }
}
