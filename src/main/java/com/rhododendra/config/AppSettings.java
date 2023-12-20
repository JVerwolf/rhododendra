package com.rhododendra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppSettings {
    @Value("${domain}")
    public String domain;
}
