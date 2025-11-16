package com.macode101.shortenerurl.service;

import com.macode101.shortenerurl.dto.AuthResponse;
import com.macode101.shortenerurl.dto.LoginRequest;
import com.macode101.shortenerurl.dto.RegisterRequest;
import com.macode101.shortenerurl.entity.User;
import com.macode101.shortenerurl.exception.DuplicateResourceException;
import com.macode101.shortenerurl.exception.UnauthorizedException;
import com.macode101.shortenerurl.repository.UserRepository;
import static com.macode101.shortenerurl.security.AuthorizeConstants.USER;
import com.macode101.shortenerurl.util.JwtUtil;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public AuthenticationServiceImpl(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }
    
    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }
        
        String passwordHash = passwordEncoder.encode(request.password());

        String uid = UUID.randomUUID().toString();

        User user = new User(request.email(), passwordHash, uid);
        user = userRepository.save(user);
        
        String accessToken = jwtUtil.generateToken(user.getUid(),  user.getEmail(), List.of(USER));
        
        return new AuthResponse(accessToken);
    }
    
    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        
        String accessToken = jwtUtil.generateToken(user.getUid(), user.getEmail(), List.of(USER));
        
        return new AuthResponse(accessToken);
    }
}
