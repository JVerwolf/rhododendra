package com.rhododendra.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostLoginRedirectTest {

    @Test
    void allowsPathAndQuery() {
        assertThat(PostLoginRedirect.isSafeRedirect("/")).isTrue();
        assertThat(PostLoginRedirect.isSafeRedirect("/rhodos/h1")).isTrue();
        assertThat(PostLoginRedirect.isSafeRedirect("/search?q=foo")).isTrue();
        assertThat(PostLoginRedirect.isSafeRedirect("/search?q=http://evil")).isTrue();
    }

    @Test
    void rejectsOpenRedirects() {
        assertThat(PostLoginRedirect.isSafeRedirect(null)).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("")).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("//evil.com")).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("https://evil.com")).isFalse();
        assertThat(PostLoginRedirect.isSafeRedirect("http://localhost/foo")).isFalse();
    }
}
