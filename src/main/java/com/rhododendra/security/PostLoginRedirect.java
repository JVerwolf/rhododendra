package com.rhododendra.security;

/**
 * Stores where to send the user after OAuth2 login (same-origin relative URL only).
 */
public final class PostLoginRedirect {

    public static final String SESSION_ATTRIBUTE = "com.rhododendra.postLoginRedirect";

    private PostLoginRedirect() {}

    /**
     * Allows same-origin paths and optional query string; blocks open redirects.
     */
    public static boolean isSafeRedirect(String target) {
        if (target == null || target.isBlank()) {
            return false;
        }
        if (target.indexOf('\r') >= 0 || target.indexOf('\n') >= 0) {
            return false;
        }
        int q = target.indexOf('?');
        String path = q >= 0 ? target.substring(0, q) : target;
        if (!path.startsWith("/") || path.startsWith("//")) {
            return false;
        }
        if (path.contains("://")) {
            return false;
        }
        return true;
    }
}
