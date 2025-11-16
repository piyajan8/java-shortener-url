package com.macode101.shortenerurl.service;

import com.macode101.shortenerurl.dto.AuthResponse;
import com.macode101.shortenerurl.dto.LoginRequest;
import com.macode101.shortenerurl.dto.RegisterRequest;
import com.macode101.shortenerurl.entity.User;
import com.macode101.shortenerurl.exception.DuplicateResourceException;
import com.macode101.shortenerurl.exception.UnauthorizedException;
import com.macode101.shortenerurl.repository.UserRepository;
import com.macode101.shortenerurl.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
    }

    @Test
    void registerWithValidRequestShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");
        User savedUser = new User("test@example.com", "hashedPassword", "test-uid");
        savedUser.setId(1L);
        
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyList())).thenReturn("test-token");

        AuthResponse response = authenticationService.register(request);

        assertNotNull(response);
        assertEquals("test-token", response.accessToken());
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(anyString(), eq("test@example.com"), anyList());
    }

    @Test
    void registerWithDuplicateEmailShouldThrowDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authenticationService.register(request));
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithValidCredentialsShouldReturnToken() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        String hashedPassword = passwordEncoder.encode("password123");
        User user = new User("test@example.com", hashedPassword, "test-uid");
        user.setId(1L);
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString(), anyList())).thenReturn("test-token");

        AuthResponse response = authenticationService.login(request);

        assertNotNull(response);
        assertEquals("test-token", response.accessToken());
        verify(userRepository).findByEmail(request.email());
        verify(jwtUtil).generateToken(eq("test-uid"), eq("test@example.com"), anyList());
    }

    @Test
    void login_WithInvalidEmail_ShouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(request));
        verify(userRepository).findByEmail(request.email());
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyList());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        String hashedPassword = passwordEncoder.encode("correctpassword");
        User user = new User("test@example.com", hashedPassword, "test-uid");
        
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(request));
        verify(userRepository).findByEmail(request.email());
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyList());
    }
}
