package com.rhododendra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

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
        OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver
    ) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(oauth2AuthorizationRequestResolver))
                .defaultSuccessUrl("/", true))
            .logout(logout -> logout.logoutSuccessUrl("/"));
        return http.build();
    }
}
