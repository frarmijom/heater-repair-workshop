package com.heaterworkshop.infrastructure.config;

import com.heaterworkshop.infrastructure.persistence.SpringDataUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

import java.util.List;
import java.util.Locale;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean UserDetailsService userDetailsService(SpringDataUserRepository repository) {
        return email -> repository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(user -> User.withUsername(user.getEmail()).password(user.getPasswordHash())
                        .authorities(List.of()).disabled(!user.isEnabled()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));
    }

    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
            CsrfTokenRepository csrf, @Value("${server.servlet.session.cookie.secure}") boolean secure) throws Exception {
        return http
                .securityContext(config -> config.securityContextRepository(contexts))
                .requestCache(config -> config.disable())
                .csrf(config -> config.csrfTokenRepository(csrf))
                .authorizeHttpRequests(config -> config
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> {
                            var authentication = SecurityContextHolder.getContext().getAuthentication();
                            response.setStatus(authentication == null || authentication instanceof AnonymousAuthenticationToken
                                    ? 401 : 403);
                        }))
                .logout(config -> config.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.addHeader("Set-Cookie", ResponseCookie.from("WORKSHOP_SESSION", "")
                                    .path("/").httpOnly(true).secure(secure).sameSite("Lax")
                                    .maxAge(0).build().toString());
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        }))
                .build();
    }
}
