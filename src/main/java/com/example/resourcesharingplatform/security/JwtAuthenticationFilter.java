package com.example.resourcesharingplatform.security;

import com.example.resourcesharingplatform.entity.User;
import com.example.resourcesharingplatform.repository.UserRepository;
import com.example.resourcesharingplatform.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getJwtFromRequest(request);

        if (StringUtils.hasText(token)) {
            try {
                String username = jwtUtil.extractUsername(token);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userRepository.findByUsername(username).orElse(null);
                    
                    if (user != null && jwtUtil.validateToken(token, username)) {
                        Integer role = jwtUtil.extractRole(token);
                        String authority = role != null && role == 1 ? "ROLE_ADMIN" : "ROLE_USER";
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                user, 
                                null, 
                                Collections.singletonList(new SimpleGrantedAuthority(authority))
                            );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                log.error("JWT认证失败: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}