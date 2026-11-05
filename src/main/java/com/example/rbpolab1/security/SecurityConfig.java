package com.example.rbpolab1.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            DatabaseUserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {

        // Authentication is checked on every request; the session stores only the CSRF token.
        RequestAttributeSecurityContextRepository contexts =
                new RequestAttributeSecurityContextRepository();

        http
                .authenticationProvider(authenticationProvider)
                .securityContext(context -> context.securityContextRepository(contexts))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(basic -> basic
                        .securityContextRepository(contexts)
                        .authenticationEntryPoint((request, response, exception) ->
                                authenticationRequired(response)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                authenticationRequired(response))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                        exception instanceof CsrfException
                                                ? "CSRF token missing or invalid"
                                                : "Access denied")))
                .csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .authorizeHttpRequests(access -> access
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/api/routes/**", "/api/stops/**",
                                "/api/shipment-assignments/**", "/api/accounts/**")
                        .hasRole("DISPATCHER")
                        .requestMatchers("/api/users/**", "/api/shipments/**", "/api/auth/me")
                        .hasAnyRole("SENDER", "DISPATCHER")
                        .anyRequest().authenticated());

        return http.build();
    }

    private void authenticationRequired(HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"delivery\", charset=\"UTF-8\"");
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Authentication required or invalid credentials");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + message
                + "\",\"timestamp\":\"" + Instant.now() + "\"}");
    }
}