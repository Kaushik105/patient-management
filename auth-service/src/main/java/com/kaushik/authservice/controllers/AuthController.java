package com.kaushik.authservice.controllers;

import com.kaushik.authservice.dto.LoginRequestDto;
import com.kaushik.authservice.dto.LoginResponseDto;
import com.kaushik.authservice.dto.RegisterRequestDto;
import com.kaushik.authservice.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT token")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {

        return authService.authenticate(loginRequestDto)
                .map(token -> ResponseEntity.ok(new LoginResponseDto(token)))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<Void> register(@RequestBody RegisterRequestDto registerRequestDto) {
        try{
            authService.register(registerRequestDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).build();
        }

        return ResponseEntity.ok().build();

    }

    @PostMapping("/validate")
    @Operation(summary = "Validate JWT token")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(400).build();
        }

        boolean isValid = authService.validateToken(authHeader.substring(7));
        log.info("Token validation result: {}", isValid);
        log.info("Received token: {}", authHeader.substring(7));

        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(401).build();
        }
    }
}
