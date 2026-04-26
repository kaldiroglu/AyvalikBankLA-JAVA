package dev.kaldiroglu.layered.ayvalikbank.exception;

public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) {
        super(message);
    }
}
