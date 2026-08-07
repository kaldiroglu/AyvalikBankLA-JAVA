package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authenticates a test as a real {@code BankUserPrincipal} carrying a customer id.
 *
 * <p>{@code @WithMockUser} builds a plain Spring {@code User}, so a controller parameter declared
 * {@code @AuthenticationPrincipal BankUserPrincipal} resolves to {@code null} under it.
 *
 * <p>Mirrors AyvalikBankHA-JAVA Refactorings.md entry 3.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithBankUserSecurityContextFactory.class)
public @interface WithBankUser {
    String customerId();
    String email() default "customer@ayvalikbank.dev";
    String role() default "CUSTOMER";
}
