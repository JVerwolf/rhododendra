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
