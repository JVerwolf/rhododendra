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

package com.rhododendra.util;

import com.rhododendra.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
    private static Logger logger = LoggerFactory.getLogger(Util.class);

    public static String getfirstLetterForIndexing(String name) {
        try {
            var firstLetter = name.substring(0, 1).toLowerCase();
            if (!firstLetter.matches("[a-z]")) {
                logger.warn("Non alphabetic first character found: " + firstLetter + ", name: " + name);
            }
            return firstLetter;
        } catch (Exception e) {
            return "a";
        }
    }

}
