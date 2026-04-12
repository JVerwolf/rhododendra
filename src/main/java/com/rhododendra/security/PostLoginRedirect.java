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

package com.rhododendra.security;

/**
 * Stores where to send the user after OAuth2 login (same-origin relative URL only).
 */
public final class PostLoginRedirect {

    public static final String SESSION_ATTRIBUTE = "com.rhododendra.postLoginRedirect";

    private PostLoginRedirect() {}

    /**
     * Allows same-origin paths and optional query string; blocks open redirects.
     */
    public static boolean isSafeRedirect(String target) {
        if (target == null || target.isBlank()) {
            return false;
        }
        if (target.indexOf('\r') >= 0 || target.indexOf('\n') >= 0) {
            return false;
        }
        int q = target.indexOf('?');
        String path = q >= 0 ? target.substring(0, q) : target;
        if (!path.startsWith("/") || path.startsWith("//")) {
            return false;
        }
        if (path.contains("://")) {
            return false;
        }
        return true;
    }
}
