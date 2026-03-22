package com.rhododendra.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * After logout, Google/Facebook often still have an active browser session and will silently
 * re-approve the same account. Extra authorization parameters force an account chooser (Google)
 * or a fresh Facebook login so users can switch accounts or providers intentionally.
 */
public final class ReauthenticationOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public ReauthenticationOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, AUTHORIZATION_REQUEST_BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest resolved = delegate.resolve(request);
        return customize(resolved, registrationIdFrom(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest resolved = delegate.resolve(request, clientRegistrationId);
        return customize(resolved, clientRegistrationId);
    }

    private static String registrationIdFrom(HttpServletRequest request) {
        String path = request.getRequestURI();
        String prefix = AUTHORIZATION_REQUEST_BASE_URI + "/";
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return null;
    }

    private static OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest request, String registrationId) {
        if (request == null || registrationId == null) {
            return request;
        }
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(request);
        switch (registrationId) {
            case "google" -> builder.additionalParameters(params -> params.put("prompt", "select_account"));
            case "facebook" -> builder.additionalParameters(params -> params.put("auth_type", "reauthenticate"));
            default -> {
            }
        }
        return builder.build();
    }
}
