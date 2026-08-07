package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.config.BankUserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;
import java.util.UUID;

public class WithBankUserSecurityContextFactory implements WithSecurityContextFactory<WithBankUser> {

    @Override
    public SecurityContext createSecurityContext(WithBankUser annotation) {
        BankUserPrincipal principal = new BankUserPrincipal(
                UUID.fromString(annotation.customerId()),
                annotation.email(),
                "test-password-hash",
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.getPassword(), principal.getAuthorities()));
        return context;
    }
}
