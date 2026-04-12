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

public class ResolvedPhotoDetails {
    public String resolvedNormalURL;
    public String resolvedHiResURL;
    public String resolvedTagURL;
    public PhotoDetails photoDetails;

    public ResolvedPhotoDetails(
        String resolvedNormalURL,
        String resolvedHiResURL,
        String resolvedTagURL,
        PhotoDetails photoDetails
    ) {
        this.resolvedNormalURL = resolvedNormalURL;
        this.resolvedHiResURL = resolvedHiResURL;
        this.resolvedTagURL = resolvedTagURL;
        this.photoDetails = photoDetails;
    }

    public String getResolvedNormalURL() {
        return resolvedNormalURL;
    }

    public String getResolvedHiResURL() {
        return resolvedHiResURL;
    }

    public String getResolvedTagURL() {
        return resolvedTagURL;
    }

    public PhotoDetails getPhotoDetails() {
        return photoDetails;
    }
}
