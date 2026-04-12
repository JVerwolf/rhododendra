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

import com.rhododendra.security.PostLoginRedirect;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PostLogoutSuccessHandler implements LogoutSuccessHandler {

    /** Form field on POST {@code /logout} (session is cleared, so redirect target must be submitted). */
    public static final String CONTINUE_PARAM = "continue";

    @Override
    public void onLogoutSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        String target = request.getParameter(CONTINUE_PARAM);
        if (target != null && PostLoginRedirect.isSafeRedirect(target)) {
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + target));
        } else {
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/"));
        }
    }
}
