package com.heaterworkshop.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions;

    public AuthenticationController(AuthenticationManager manager, SecurityContextRepository contexts,
                                    SessionAuthenticationStrategy sessions) {
        this.manager = manager;
        this.contexts = contexts;
        this.sessions = sessions;
    }

    // Do not use a record: its generated toString would include the password.
    public static final class LoginRequest {
        public String email;
        public String password;
    }

    public record SessionResponse(String email) { }
    public record CsrfResponse(String headerName, String token) { }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            // Bean-validation exceptions can log rejected field values, including passwords.
            if (credentials.email == null || credentials.email.isBlank() || credentials.email.length() > 254
                    || credentials.password == null || credentials.password.isBlank()
                    || credentials.password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                return ResponseEntity.status(401).body(java.util.Map.of("message", "Invalid credentials."));
            }
            Authentication authentication = manager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(credentials.email, credentials.password));
            sessions.onAuthentication(authentication, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            return ResponseEntity.ok(new SessionResponse(authentication.getName()));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Invalid credentials."));
        } catch (IllegalArgumentException exception) {
            // A malformed administratively supplied hash must not expose encoder diagnostics.
            return ResponseEntity.internalServerError().body(java.util.Map.of("message", "Unable to complete the request."));
        } finally {
            credentials.password = null;
        }
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication) {
        return new SessionResponse(authentication.getName());
    }
}
