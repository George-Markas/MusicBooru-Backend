package com.example.musicbooru.auth;

import com.example.musicbooru.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<?> getUserRole(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authenticationService.getUserRole(user));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        AuthenticationResponse authenticationResponse = authenticationService.register(request);
        return ResponseEntity.status(authenticationResponse.status())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.cookieString())
                .body(authenticationResponse);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse authenticationResponse = authenticationService.authenticate(request);
        return ResponseEntity.status(authenticationResponse.status())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.cookieString())
                .body(authenticationResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        AuthenticationResponse authenticationResponse = authenticationService.logout();
        return ResponseEntity.status(authenticationResponse.status())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.cookieString())
                .body(authenticationResponse);
    }
}
