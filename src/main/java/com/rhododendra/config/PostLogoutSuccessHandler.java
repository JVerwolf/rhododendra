package com.rhododendra.config;

import com.rhododendra.security.PostLoginRedirect;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PostLogoutSuccessHandler implements LogoutSuccessHandler {

    /** Form field on POST {@code /logout} (session is cleared, so redirect target must be submitted). */
    public static final String CONTINUE_PARAM = "continue";

    @Override
    public void onLogoutSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        String target = request.getParameter(CONTINUE_PARAM);
        if (target != null && PostLoginRedirect.isSafeRedirect(target)) {
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + target));
        } else {
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/"));
        }
    }
}
