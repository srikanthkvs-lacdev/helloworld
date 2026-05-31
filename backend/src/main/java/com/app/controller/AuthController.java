package com.app.controller;

import com.app.dto.LoginRequest;
import com.app.dto.LoginResponse;
import com.app.dto.UserDto;
import com.app.model.User;
import com.app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepo;

    public AuthController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        Optional<User> result = userRepo.findByEmailAndPassword(req.getEmail(), req.getPassword());

        if (result.isPresent()) {
            return ResponseEntity.ok(new LoginResponse(true, "Login successful", new UserDto(result.get())));
        }

        return ResponseEntity.status(401)
            .body(new LoginResponse(false, "Invalid email or password", null));
    }
}
