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
