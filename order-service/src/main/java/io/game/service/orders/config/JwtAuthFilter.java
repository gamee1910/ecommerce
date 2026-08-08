package io.game.service.orders.config;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "JwtAuthFilter")
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String userRole = request.getHeader(HEADER_USER_ROLE);
        String userEmail = request.getHeader(HEADER_USER_EMAIL);

        if (userId != null && userRole != null) {
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(userRole));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, userEmail, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user {} with role {}", userId, userRole);
        }

        filterChain.doFilter(request, response);
    }
}
