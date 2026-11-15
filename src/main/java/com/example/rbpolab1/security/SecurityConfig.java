package com.example.rbpolab1.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

import java.time.Clock;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

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
            HttpSecurity http, DaoAuthenticationProvider authenticationProvider,
            JwtTokenProvider jwt, DatabaseUserDetailsService users) throws Exception {

        http
                .authenticationProvider(authenticationProvider)
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                AuthenticationErrors.unauthorized(response))
                        .accessDeniedHandler((request, response, exception) ->
                                AuthenticationErrors.write(response, HttpServletResponse.SC_FORBIDDEN, "Access denied")))
                // Authentication uses explicit Bearer headers and JSON, with no authentication cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(access -> access
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register", "/auth/refresh").permitAll()
                        .requestMatchers("/api/routes/**", "/api/stops/**",
                                "/api/shipment-assignments/**", "/api/accounts/**")
                        .hasRole("DISPATCHER")
                        .requestMatchers("/api/users/**", "/api/shipments/**", "/api/auth/me")
                        .hasAnyRole("SENDER", "DISPATCHER")
                        .anyRequest().authenticated())
                // Created only here, so it is not also registered as a servlet filter.
                .addFilterBefore(new JwtTokenFilter(jwt, users), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
