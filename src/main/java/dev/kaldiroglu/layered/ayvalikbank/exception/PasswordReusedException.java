package dev.kaldiroglu.layered.ayvalikbank.exception;

public class PasswordReusedException extends RuntimeException {
    public PasswordReusedException(String message) {
        super(message);
    }
}
