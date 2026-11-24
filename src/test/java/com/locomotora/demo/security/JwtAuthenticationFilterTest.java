package com.locomotora.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.security.AccountStatusService.AccountState;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** Un token valido no alcanza: la cuenta tiene que seguir activa, y el rol sale de la base. */
class JwtAuthenticationFilterTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private final JwtService jwtService = mock(JwtService.class);
    private final AccountStatusService accounts = mock(AccountStatusService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, accounts);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/me");
        request.addHeader("Authorization", "Bearer token");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        when(jwtService.validateAndGetUserId("token")).thenReturn(USER_ID);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aDisabledAccountGets401EvenWithAValidToken() throws Exception {
        when(accounts.find(USER_ID)).thenReturn(Optional.of(new AccountState("DISABLED", "USER")));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains(JwtAuthenticationFilter.DISABLED_MESSAGE);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void aDeletedOrMissingAccountGets401() throws Exception {
        when(accounts.find(USER_ID)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void anActiveAdminIsAuthenticatedWithTheAdminRole() throws Exception {
        when(accounts.find(USER_ID)).thenReturn(Optional.of(new AccountState("ACTIVE", "ADMIN")));
        FilterChain checking = (req, res) -> {
            assertThat(CurrentUser.id()).isEqualTo(USER_ID);
            assertThat(CurrentUser.isAdmin()).isTrue();
        };

        filter.doFilter(request, response, checking);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void aRegularUserIsNotAdmin() throws Exception {
        when(accounts.find(USER_ID)).thenReturn(Optional.of(new AccountState("ACTIVE", "USER")));
        FilterChain checking = (req, res) -> assertThat(CurrentUser.isAdmin()).isFalse();

        filter.doFilter(request, response, checking);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
