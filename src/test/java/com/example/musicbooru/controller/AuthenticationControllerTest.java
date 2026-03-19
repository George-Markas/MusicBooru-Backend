package com.example.musicbooru.controller;

import com.example.musicbooru.auth.AuthenticationController;
import com.example.musicbooru.auth.AuthenticationService;
import com.example.musicbooru.auth.JwtService;
import com.example.musicbooru.model.Role;
import com.example.musicbooru.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("SpellCheckingInspection")
@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        user.setUsername("testuser");
        user.setRole(Role.USER);

        admin = new User();
        admin.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        admin.setUsername("testadmin");
        admin.setRole(Role.ADMIN);
    }

    // --- GET /api/auth ---

    @Test
    void getUserRole_returnsOkWithUserRole() throws Exception {
        when(authenticationService.getUserRole(any(User.class))).thenReturn(Role.USER);

        mockMvc.perform(get("/api/auth")
                        .with(user(user))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("USER"));
    }

    @Test
    void getUserRole_returnsOkWithAdminRole() throws Exception {
        when(authenticationService.getUserRole(any(User.class))).thenReturn(Role.ADMIN);

        mockMvc.perform(get("/api/auth")
                        .with(user(admin))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("ADMIN"));
    }

    /* TODO Test the rest of the controller methods. I genuinely could not figure out how to make
        the test environment play nice with our security config, so I will leave things as is for now. */
}