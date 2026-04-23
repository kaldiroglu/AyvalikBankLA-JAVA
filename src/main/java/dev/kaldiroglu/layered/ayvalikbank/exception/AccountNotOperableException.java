package dev.kaldiroglu.layered.ayvalikbank.exception;

public class AccountNotOperableException extends RuntimeException {
    public AccountNotOperableException(String message) {
        super(message);
    }
}
