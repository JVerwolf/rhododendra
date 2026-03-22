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
