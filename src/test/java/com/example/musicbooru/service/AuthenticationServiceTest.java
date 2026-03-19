package com.example.musicbooru.service;

import com.example.musicbooru.auth.*;
import com.example.musicbooru.dto.UserInfoResponse;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.model.Role;
import com.example.musicbooru.model.User;
import com.example.musicbooru.model.UserAuthView;
import com.example.musicbooru.repository.UserAuthViewRepository;
import com.example.musicbooru.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("SpellCheckingInspection")
@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthViewRepository authViewRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User user;
    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        user.setUsername("testuser");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);

        registerRequest = new RegisterRequest("testuser", "plain-password", Role.USER);
        authenticationRequest = new AuthenticationRequest("testuser", "plain-password");
    }

    // --- getUserInfo ---

    @Test
    void getUserInfo_returnsUserInfo() {
        UserInfoResponse result = authenticationService.getUserInfo(user);

        assertThat(result).isEqualTo(
                new UserInfoResponse("testuser", Role.USER)
        );
        // No repository interaction should occur; this is a pure delegation to the model
        verifyNoInteractions(userRepository, authViewRepository, jwtService, authenticationManager);
    }

    // --- register ---

    @Test
    void register_savesUserAndReturnsSuccessResponse() {
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.cookieFromToken("jwt-token")).thenReturn("cookie-string");

        AuthenticationResponse result = authenticationService.register(registerRequest);

        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.message()).isEqualTo("User registered");
        assertThat(result.cookieString()).isEqualTo("cookie-string");

        // Confirm the user was actually persisted
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_encodesPasswordBeforeSaving() {
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.cookieFromToken("jwt-token")).thenReturn("cookie-string");

        authenticationService.register(registerRequest);

        // Capture the User passed to save() and assert its password is the encoded form,
        // not the plain text from the request.
        verify(userRepository).save(argThat(u -> u.getPassword().equals("encoded-password")));
    }

    @Test
    void register_throwsGenericException_whenUsernameAlreadyExists() {
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(GenericException.class)
                .hasMessageContaining("Username already in use");

        // If the username is taken, the user must never be saved
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_setsCorrectRoleForUser() {
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.cookieFromToken("jwt-token")).thenReturn("cookie-string");

        authenticationService.register(registerRequest);

        verify(userRepository).save(argThat(u -> u.getRole().equals(Role.USER)));
    }

    // --- authenticate ---

    @Test
    void authenticate_returnsSuccessResponse_whenCredentialsAreValid() {
        UserAuthView authView = mock(UserAuthView.class);
        when(authViewRepository.findByUsername("testuser")).thenReturn(Optional.of(authView));
        when(jwtService.generateToken(authView)).thenReturn("jwt-token");
        when(jwtService.cookieFromToken("jwt-token")).thenReturn("cookie-string");

        AuthenticationResponse result = authenticationService.authenticate(authenticationRequest);

        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.message()).isEqualTo("Logged in");
        assertThat(result.cookieString()).isEqualTo("cookie-string");
    }

    @Test
    void authenticate_throwsGenericException_whenCredentialsAreInvalid() {
        // Simulating Spring Security rejecting the credentials
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .isInstanceOf(GenericException.class)
                .hasMessageContaining("Incorrect username or password");

        // Authentication failed before the view repository is ever consulted
        verifyNoInteractions(authViewRepository);
    }

    @Test
    void authenticate_throwsGenericException_whenUserNotFoundAfterAuthentication() {
        // Authentication passes but the view record is missing. This should not happen
        // in practice but the service guards against it anyway.
        when(authViewRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .isInstanceOf(GenericException.class)
                .hasMessageContaining("Incorrect username or password");
    }

    @Test
    void authenticate_passesCorrectCredentialsToAuthenticationManager() {
        UserAuthView authView = mock(UserAuthView.class);
        when(authViewRepository.findByUsername("testuser")).thenReturn(Optional.of(authView));
        when(jwtService.generateToken(authView)).thenReturn("jwt-token");
        when(jwtService.cookieFromToken("jwt-token")).thenReturn("cookie-string");

        authenticationService.authenticate(authenticationRequest);

        // Confirm the token passed to the manager carries the exact username
        // and password from the request, not some transformed version.
        verify(authenticationManager).authenticate(
                argThat(token ->
                        token instanceof UsernamePasswordAuthenticationToken t &&
                                t.getPrincipal().equals("testuser") &&
                                t.getCredentials().equals("plain-password")
                )
        );
    }

    // --- logout ---

    @Test
    void logout_returnsResponseWithLogoutCookie() {
        when(jwtService.logoutCookie()).thenReturn("logout-cookie-string");

        AuthenticationResponse result = authenticationService.logout();

        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.message()).isEqualTo("Logged out");
        assertThat(result.cookieString()).isEqualTo("logout-cookie-string");
        // No repository interaction should occur during logout.
        verifyNoInteractions(userRepository, authViewRepository, authenticationManager);
    }
}
