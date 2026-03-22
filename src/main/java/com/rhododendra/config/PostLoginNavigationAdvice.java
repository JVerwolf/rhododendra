package com.rhododendra.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PostLoginNavigationAdvice {

    /**
     * Path (+ optional query) within the web app, for {@code /login?continue=…} so post-OAuth redirect returns here.
     */
    @ModelAttribute("signInContinueUrl")
    public String signInContinueUrl(HttpServletRequest request) {
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        String pathWithinApp = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
        if (pathWithinApp.isEmpty()) {
            pathWithinApp = "/";
        }
        if ("/login".equals(pathWithinApp)) {
            return "/";
        }
        String q = request.getQueryString();
        if (q != null && !q.isEmpty()) {
            return pathWithinApp + "?" + q;
        }
        return pathWithinApp;
    }
}
