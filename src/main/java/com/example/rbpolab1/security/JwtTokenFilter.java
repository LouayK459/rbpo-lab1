package com.example.rbpolab1.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class JwtTokenFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_AUTH = Set.of("/auth/login", "/auth/register", "/auth/refresh");
    private final JwtTokenProvider jwt;
    private final DatabaseUserDetailsService users;

    public JwtTokenFilter(JwtTokenProvider jwt, DatabaseUserDetailsService users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "POST".equals(request.getMethod()) && PUBLIC_AUTH.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    throw new BadCredentialsException("Bearer token required");
                }
                String username = jwt.validateAccess(header.substring(7).trim()).getSubject();
                UserDetails user = users.loadUserByUsername(username);
                if (!user.isEnabled() || !user.isAccountNonLocked() || !user.isAccountNonExpired()
                        || !user.isCredentialsNonExpired()) {
                    throw new BadCredentialsException("Account is unavailable");
                }
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                AuthenticationErrors.unauthorized(response);
                return;
            }
        }
        // Keep downstream controller, authorization and database failures outside the JWT catch.
        filterChain.doFilter(request, response);
    }
}
