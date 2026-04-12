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

import static org.assertj.core.api.Assertions.assertThat;

class PostLoginRedirectTest {

    @Test
    void allowsPathAndQuery() {
        assertThat(PostLoginRedirect.isSafeRedirect("/")).isTrue();
        assertThat(PostLoginRedirect.isSafeRedirect("/rhodos/h1")).isTrue();
        assertThat(PostLoginRedirect.isSafeRedirect("/search?q=foo")).isTrue();
        assertThat(PostLoginRedirect.isSafeRedirect("/search?q=http://evil")).isTrue();
    }

    @Test
    void rejectsOpenRedirects() {
        assertThat(PostLoginRedirect.isSafeRedirect(null)).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("")).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("//evil.com")).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("https://evil.com")).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("http://localhost/foo")).isFalse();
    }
}
