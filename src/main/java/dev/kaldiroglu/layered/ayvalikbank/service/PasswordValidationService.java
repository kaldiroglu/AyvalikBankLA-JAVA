package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.InvalidPasswordException;
import org.springframework.stereotype.Service;

@Service
public class PasswordValidationService {

    public void validate(String password) {
        if (password == null)
            throw new IllegalArgumentException("Password must not be null");
        if (password.length() < 8 || password.length() > 16)
            throw new InvalidPasswordException("Password must be between 8 and 16 characters");
        if (!password.matches(".*[A-Z].*"))
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        if (!password.matches(".*[a-z].*"))
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        if (!password.matches(".*[0-9].*"))
            throw new InvalidPasswordException("Password must contain at least one digit");
        if (!password.matches(".*[^A-Za-z0-9].*"))
            throw new InvalidPasswordException("Password must contain at least one special character");
    }
}
