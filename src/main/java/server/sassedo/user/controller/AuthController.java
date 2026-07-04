package server.sassedo.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.model.GenericResponse;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.user.data.UserDetailsImpl;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.network.request.*;
import server.sassedo.user.data.network.response.LoginResponse;
import server.sassedo.user.service.user.UserService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getSiteURL;

@CrossOrigin(origins = "*", maxAge = 3600)
@Controller
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    UserService userService;

    @Value("${feria.app.clientUrl}")
    private String clientUrl;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        } catch (LockedException e) {
            return ResponseEntity.badRequest().body(new GenericException(GenericExceptionCode.USER_BLOCKED, "User is blocked").getErrorResponse());
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (userDetails.isBlocked()) {
            return ResponseEntity.badRequest().body(new GenericException(GenericExceptionCode.USER_BLOCKED, "User is blocked").getErrorResponse());
        }

        if (!userDetails.isEmailVerified()) {
            return ResponseEntity.badRequest().body(new GenericException(GenericExceptionCode.NOT_VERIFIED, "Not verified").getErrorResponse());
        }

        String jwtToken = jwtUtils.generateJwtToken(userDetails);

        LoginResponse loginResponse = new LoginResponse(jwtToken);

        return ResponseEntity.ok().body(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        try {
            userService.registerUser(signUpRequest, getSiteURL(request));
            return new ResponseEntity<>(new GenericResponse("User registered successfully!"), HttpStatus.CREATED);
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        // Optionally, you can also invalidate the session if it exists
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ResponseEntity.ok()
                .body(new GenericResponse("You've been signed out!"));
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody VerificationMailRequest verificationMailRequest) throws MessagingException, UnsupportedEncodingException {
        userService.sendVerificationCode(verificationMailRequest.getEmail());
        return ResponseEntity.ok()
                .body(new GenericResponse("Verification code sent successfully!"));
    }

    @PutMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserRequest verifyUserRequest) {
        try {
            userService.verify(verifyUserRequest);
            User user = userService.getUserByEmail(verifyUserRequest.getEmail());
            List<GrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                    .collect(Collectors.toList());
            UserDetailsImpl userDetails = new UserDetailsImpl(
                    user.getId(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getName(),
                    user.isEnabled(),
                    user.isBlocked(),
                    authorities
            );

            // Generate JWT token
            String jwtToken = jwtUtils.generateJwtToken(userDetails);

            return ResponseEntity.ok()
                    .body(new LoginResponse(jwtToken));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }
}