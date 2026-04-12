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

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2UserAttributesTest {

    @Test
    void emailAndNameFromGoogleStyleAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "google-sub");
        attrs.put("email", "a@b.com");
        attrs.put("name", "Ada Lovelace");
        var user = new DefaultOAuth2User(
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            attrs,
            "sub"
        );
        assertThat(OAuth2UserAttributes.email(user)).isEqualTo("a@b.com");
        assertThat(OAuth2UserAttributes.displayName(user)).isEqualTo("Ada Lovelace");
    }

    @Test
    void displayNameFallsBackWhenEmailMissing() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", "facebook-subject");
        attrs.put("name", "Test User");
        var user = new DefaultOAuth2User(
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            attrs,
            "id"
        );
        assertThat(OAuth2UserAttributes.email(user)).isNull();
        assertThat(OAuth2UserAttributes.displayName(user)).isEqualTo("Test User");
    }
}
