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

package com.rhododendra.config;

import com.rhododendra.security.OAuth2UserAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AuthModelAdvice {

    @ModelAttribute("oauthLoggedIn")
    public boolean oauthLoggedIn(@AuthenticationPrincipal OAuth2User user) {
        return user != null;
    }

    @ModelAttribute("oauthDisplayName")
    public String oauthDisplayName(@AuthenticationPrincipal OAuth2User user) {
        return OAuth2UserAttributes.displayName(user);
    }

    @ModelAttribute("oauthEmail")
    public String oauthEmail(@AuthenticationPrincipal OAuth2User user) {
        return OAuth2UserAttributes.email(user);
    }
}
