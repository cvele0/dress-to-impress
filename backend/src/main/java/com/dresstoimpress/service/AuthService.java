package com.dresstoimpress.service;

import com.dresstoimpress.exception.UserNotRegisteredException;
import com.dresstoimpress.exception.UserNotVerifiedException;
import com.dresstoimpress.responses.ErrorResponse;
import com.dresstoimpress.responses.JwtResponse;
import com.dresstoimpress.requests.LoginRequest;
import com.dresstoimpress.model.User;
import com.dresstoimpress.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager, UserService userService,
                       EmailVerificationService emailVerificationService,
                       BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public ResponseEntity<?> login(LoginRequest loginRequest) {
        User user = userService.findByEmail(loginRequest.getEmail());
        try {
            checkUserForLogin(user, loginRequest);
            handleAuthentication(loginRequest);
            return ResponseEntity.ok(new JwtResponse(jwtUtil.generateJwt(loginRequest.getEmail())));
        } catch (UserNotVerifiedException e) {
            handleUserNotVerified(user);
            return ResponseEntity.status(401).body(new ErrorResponse("Email hasn't been verified!"));
        }
    }

    private void checkUserForLogin(User user, LoginRequest loginRequest)
            throws UserNotRegisteredException, UserNotVerifiedException {
        if (user == null) {
            throw new UserNotRegisteredException(
                    "User with email " + loginRequest.getEmail() + " is not registered!",
                    loginRequest.getEmail()
            );
        } else if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()) && !user.isEmailVerified()) {
            throw new UserNotVerifiedException("User with email " + loginRequest.getEmail() + " is not verified!");
        }
    }

    private void handleAuthentication(LoginRequest loginRequest) throws AuthenticationException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        LOGGER.info("Authentication is successful: {}", loginRequest.getEmail());
    }

    private void handleUserNotVerified(User user) {
        LOGGER.error("Email hasn't been verified: {}", user.getEmail());
        emailVerificationService.deleteByUserId(user.getId()); // Remove old token
        emailVerificationService.createAndSendVerificationToken(user); // Send new token
    }
}