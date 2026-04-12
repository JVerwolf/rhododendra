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

import org.springframework.beans.factory.annotation.Value;
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
        @Value("${app.sign-in.enabled:false}") boolean signInEnabled,
        OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver,
        AuthenticationSuccessHandler postLoginOAuth2SuccessHandler,
        LogoutSuccessHandler postLogoutSuccessHandler
    ) throws Exception {
        if (signInEnabled) {
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
        } else {
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/rhodos/*/edit").denyAll()
                .anyRequest().permitAll());
        }
        return http.build();
    }
}
