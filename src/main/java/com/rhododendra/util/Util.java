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
