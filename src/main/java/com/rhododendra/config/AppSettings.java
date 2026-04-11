package com.rhododendra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppSettings {
    @Value("${domain}")
    public String domain;

    @Value("${app.sign-in.enabled:false}")
    private boolean signInEnabled;

    public boolean isSignInEnabled() {
        return signInEnabled;
    }
}
