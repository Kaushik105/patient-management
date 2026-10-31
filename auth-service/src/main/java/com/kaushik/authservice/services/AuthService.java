package com.kaushik.authservice.services;

import com.kaushik.authservice.dto.LoginRequestDto;
import com.kaushik.authservice.dto.RegisterRequestDto;
import com.kaushik.authservice.exception.BadCredentialsException;
import com.kaushik.authservice.exception.UserAlreadyExistsException;
import com.kaushik.authservice.exception.UserNotFoundException;
import com.kaushik.authservice.model.User;
import com.kaushik.authservice.util.JwtUtil;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // Register service
    public void register(RegisterRequestDto registerRequestDto) {
        // Check if user already exists
        String email = registerRequestDto.getEmail();
        String password = registerRequestDto.getPassword();
        String role = registerRequestDto.getRole();

        Optional<User> existingUser = userService.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User already exists with email: " + email);
        }

        // Create new user and save to database
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role);
        userService.save(newUser);
    }

    public Optional<String> authenticate(LoginRequestDto loginRequestDto) {

        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();
        // Validate user credentials
        Optional<User> userOptional = userService.findByEmail(email);
        if (!userOptional.isPresent()) {
            throw new UserNotFoundException("User not found with email: " + email);
        }

        return Optional.of(
                userOptional
                        .filter((u -> passwordEncoder.matches(password, u.getPassword())))
                        .map(u -> jwtUtil.generateToken(u.getRole(), u.getEmail()))
                        .orElseThrow(
                                () ->
                                        new BadCredentialsException(
                                                "Invalid password for email: " + email)));
    }

    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return true;
        } catch (Exception e) {
            log.info(e.getMessage());
            return false;
        }
    }
}
