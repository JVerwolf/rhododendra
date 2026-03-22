package com.rhododendra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/** OAuth2 login is servlet-only; the headless {@code migrate} profile has no {@code ClientRegistrationRepository} bean. */
@Configuration
@EnableWebSecurity
@Profile("!migrate")
public class SecurityConfiguration {

    @Bean
    public OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new ReauthenticationOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver,
        AuthenticationSuccessHandler postLoginOAuth2SuccessHandler,
        LogoutSuccessHandler postLogoutSuccessHandler
    ) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/rhodos/*/edit").authenticated()
                .anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(oauth2AuthorizationRequestResolver))
                .successHandler(postLoginOAuth2SuccessHandler))
            .logout(logout -> logout.logoutSuccessHandler(postLogoutSuccessHandler));
        return http.build();
    }
}
