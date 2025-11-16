package com.macode101.shortenerurl.service;

import com.macode101.shortenerurl.dto.AuthResponse;
import com.macode101.shortenerurl.dto.LoginRequest;
import com.macode101.shortenerurl.dto.RegisterRequest;

public interface AuthenticationService {
    
    AuthResponse register(RegisterRequest request);
    
    AuthResponse login(LoginRequest request);
}
