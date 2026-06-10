package com.cinebook.service;

import com.cinebook.dto.AuthResponse;
import com.cinebook.dto.LoginRequest;
import com.cinebook.dto.RegisterAdminRequest;
import com.cinebook.dto.RegisterRequest;
import com.cinebook.entity.Role;
import com.cinebook.entity.Theater;
import com.cinebook.entity.User;
import com.cinebook.exception.ApiException;
import com.cinebook.repository.TheaterRepository;
import com.cinebook.repository.UserRepository;
import com.cinebook.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TheaterRepository theaterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, TheaterRepository theaterRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.theaterRepository = theaterRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Register a regular moviegoer (ROLE USER). */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        ensureUsernameAvailable(request.getUsername());

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user = userRepository.save(user);

        return toResponse(user);
    }

    /** Register a theater owner (ROLE ADMIN) and create their theater. */
    @Transactional
    public AuthResponse registerAdmin(RegisterAdminRequest request) {
        ensureUsernameAvailable(request.getUsername());

        User admin = new User();
        admin.setUsername(request.getUsername().trim());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);

        Theater theater = new Theater();
        theater.setName(request.getTheaterName().trim());
        theater.setLocation(request.getTheaterLocation() == null ? null : request.getTheaterLocation().trim());
        theater.setOwnerUserId(admin.getId());
        theater = theaterRepository.save(theater);

        admin.setTheaterId(theater.getId());
        admin = userRepository.save(admin);

        return toResponse(admin);
    }

    /** Authenticate by plaintext password comparison. */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid username or password");
        }

        return toResponse(user);
    }

    private void ensureUsernameAvailable(String username) {
        if (username == null || username.isBlank()) {
            throw ApiException.badRequest("Username is required");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw ApiException.conflict("Username already taken");
        }
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(user.getId(), user.getUsername(), user.getRole(),
                user.getTheaterId(), token);
    }
}
