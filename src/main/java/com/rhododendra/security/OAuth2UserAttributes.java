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

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * Normalizes name and email from Google and Facebook OAuth2 userinfo payloads.
 */
public final class OAuth2UserAttributes {

    private OAuth2UserAttributes() {}

    public static String email(OAuth2User user) {
        if (user == null) {
            return null;
        }
        String email = user.getAttribute("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        Map<String, Object> attrs = user.getAttributes();
        if (attrs != null) {
            Object nested = attrs.get("email");
            if (nested instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    public static String displayName(OAuth2User user) {
        if (user == null) {
            return null;
        }
        String name = user.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String given = user.getAttribute("given_name");
        String family = user.getAttribute("family_name");
        if (given != null && !given.isBlank()) {
            return family != null && !family.isBlank() ? given + " " + family : given;
        }
        String email = email(user);
        if (email != null) {
            return email;
        }
        return user.getName();
    }
}
