package com.locomotora.demo.security;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.security.AccountStatusService.AccountState;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    static final String DISABLED_MESSAGE = "Account is disabled";

    private final JwtService jwtService;
    private final AccountStatusService accountStatusService;

    public JwtAuthenticationFilter(JwtService jwtService, AccountStatusService accountStatusService) {
        this.jwtService = jwtService;
        this.accountStatusService = accountStatusService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UUID userId = jwtService.validateAndGetUserId(header.substring(7));
                // El token sigue siendo valido aunque la cuenta se haya inhabilitado: se revisa el estado actual.
                // 401 hace que la app cierre la sesion.
                AccountState account = accountStatusService.find(userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
                if (!account.isActive()) {
                    throw new ApiException(HttpStatus.UNAUTHORIZED, DISABLED_MESSAGE);
                }
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + account.role()))
                ));
            } catch (ApiException exception) {
                SecurityContextHolder.clearContext();
                response.setStatus(exception.status().value());
                response.setContentType("application/json");
                response.getWriter().write("""
                        {"timestamp":"%s","status":%d,"message":"%s"}
                        """.formatted(Instant.now(), exception.status().value(), exception.getMessage()).trim());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
