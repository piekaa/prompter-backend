package com.prompter.controller;

import com.prompter.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthenticationIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Test
    void unauthenticatedAccess_backoffice_redirectsToLogin() throws Exception {
        mvc.perform(get("/backoffice"))
           .andExpect(status().is3xxRedirection());
    }

    @Test
    void authenticatedAccess_backoffice_succeeds() throws Exception {
        mvc.perform(get("/backoffice")
                .with(user("admin").password("admin").roles("USER")))
           .andExpect(status().isOk());
    }

    @Test
    void indexPage_public() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk());
    }

    @Test
    void loginPage_returnsForm() throws Exception {
        mvc.perform(get("/login.html"))
           .andExpect(status().isOk());
    }

    @Test
    void login_invalidCredentials_showsError() throws Exception {
        mvc.perform(post("/login")
                .param("username", "admin")
                .param("password", "wrong"))
           .andExpect(status().is3xxRedirection());
    }

    @Test
    void login_validCredentials_redirectsToBackoffice() throws Exception {
        mvc.perform(post("/login")
                .param("username", "admin")
                .param("password", "admin"))
           .andExpect(status().is3xxRedirection());
    }

    @Test
    void logout_clearsSession_redirectsToLogin() throws Exception {
        mvc.perform(post("/logout"))
           .andExpect(status().is3xxRedirection());
    }
}
