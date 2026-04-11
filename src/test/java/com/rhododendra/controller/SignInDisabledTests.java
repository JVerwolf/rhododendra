package com.rhododendra.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.sign-in.enabled=false",
    "db.path=build/test-rhododendra-signin-disabled.sqlite"
})
class SignInDisabledTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageReturnsNotFound() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isNotFound());
    }

    @Test
    void postEditReturnsForbiddenWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/rhodos/1/edit")
                .with(csrf())
                .param("ten_year_height", "9m"))
            .andExpect(status().isForbidden());
    }
}
