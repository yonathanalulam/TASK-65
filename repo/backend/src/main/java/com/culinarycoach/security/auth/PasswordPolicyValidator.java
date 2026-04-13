package com.culinarycoach.security.auth;

import com.culinarycoach.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordPolicyValidator {

    private final AppProperties appProperties;

    public PasswordPolicyValidator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public List<String> validate(String password, String username) {
        List<String> errors = new ArrayList<>();
        int minLength = appProperties.getSecurity().getPasswordMinLength();

        if (password == null || password.length() < minLength) {
            errors.add("Password must be at least " + minLength + " characters");
            return errors;
        }

        if (!password.matches(".*[A-Z].*")) {
            errors.add("Password must contain at least 1 uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            errors.add("Password must contain at least 1 lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            errors.add("Password must contain at least 1 digit");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            errors.add("Password must contain at least 1 non-alphanumeric character");
        }

        if (username != null && username.length() >= 4) {
            String lowerPassword = password.toLowerCase();
            String lowerUsername = username.toLowerCase();
            if (lowerPassword.contains(lowerUsername)) {
                errors.add("Password must not contain the username");
            }
        }

        return errors;
    }
}
