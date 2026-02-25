package com.aliyaman.icqa_shein.security.auth;



import com.aliyaman.icqa_shein.security.auth.dto.LoginRequest;
import com.aliyaman.icqa_shein.security.auth.dto.LoginResponse;
import com.aliyaman.icqa_shein.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authManager, UserDetailsService userDetailsService, JwtService jwtService) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        UserDetails user = userDetailsService.loadUserByUsername(req.getUsername());
        String token = jwtService.generateAccessToken(user);

        List<String> roles = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return new LoginResponse(token, "Bearer", roles);
    }
}
