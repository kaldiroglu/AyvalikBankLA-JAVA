package dev.kaldiroglu.layered.ayvalikbank.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * The authenticated principal, carrying the caller's customer id.
 *
 * <p>Spring Security identifies users by username — here, the email address. Authorization rules
 * need the customer id, and resolving email to id per request would mean a database query on every
 * authorized call to recover something login already knew: {@link BankUserDetailsService} loads the
 * whole customer in order to read its password hash.
 *
 * <p>Mirrors AyvalikBankHA-JAVA {@code Refactorings.md} entry 3.
 */
public class BankUserPrincipal extends User {

    private final transient UUID customerId;

    public BankUserPrincipal(UUID customerId, String email, String passwordHash,
                             Collection<? extends GrantedAuthority> authorities) {
        super(email, passwordHash, authorities);
        this.customerId = customerId;
    }

    public UUID customerId() {
        return customerId;
    }
}
