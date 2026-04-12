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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PostLoginNavigationAdvice {

    private final AppSettings appSettings;

    public PostLoginNavigationAdvice(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    @ModelAttribute("signInEnabled")
    public boolean signInEnabled() {
        return appSettings.isSignInEnabled();
    }

    /**
     * Path (+ optional query) within the web app, for {@code /login?continue=…} so post-OAuth redirect returns here.
     */
    @ModelAttribute("signInContinueUrl")
    public String signInContinueUrl(HttpServletRequest request) {
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        String pathWithinApp = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
        if (pathWithinApp.isEmpty()) {
            pathWithinApp = "/";
        }
        if ("/login".equals(pathWithinApp)) {
            return "/";
        }
        String q = request.getQueryString();
        if (q != null && !q.isEmpty()) {
            return pathWithinApp + "?" + q;
        }
        return pathWithinApp;
    }
}
