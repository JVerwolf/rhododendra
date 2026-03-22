package com.rhododendra.config;

import com.rhododendra.security.PostLoginRedirect;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PostLoginOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public PostLoginOAuth2SuccessHandler() {
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object raw = session.getAttribute(PostLoginRedirect.SESSION_ATTRIBUTE);
            if (raw instanceof String target && PostLoginRedirect.isSafeRedirect(target)) {
                session.removeAttribute(PostLoginRedirect.SESSION_ATTRIBUTE);
                getRedirectStrategy().sendRedirect(request, response, request.getContextPath() + target);
                return;
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
