package edu.anurag.complaints.service;

import edu.anurag.complaints.dto.Dtos.*;
import edu.anurag.complaints.model.Role;
import edu.anurag.complaints.model.User;
import edu.anurag.complaints.repository.UserRepository;
import edu.anurag.complaints.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setPassword(encoder.encode(req.password()));
        u.setRole(Role.STUDENT); // public registration can never create an admin
        u.setRollNumber(req.rollNumber() == null ? null : req.rollNumber().trim());
        u.setDepartment(req.department() == null ? null : req.department().trim());
        users.save(u);
        return new AuthResponse(jwt.generate(u), UserResponse.from(u));
    }

    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .filter(found -> encoder.matches(req.password(), found.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u), UserResponse.from(u));
    }

    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
}
