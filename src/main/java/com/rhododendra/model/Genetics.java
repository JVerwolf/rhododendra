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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Genetics {
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
