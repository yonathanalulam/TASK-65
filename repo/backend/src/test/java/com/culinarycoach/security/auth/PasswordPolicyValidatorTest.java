package com.culinarycoach.security.auth;

import com.culinarycoach.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getSecurity().setPasswordMinLength(12);
        validator = new PasswordPolicyValidator(props);
    }

    @Test
    void validPassword_noErrors() {
        List<String> errors = validator.validate("StrongPass1!xy", "testuser");
        assertTrue(errors.isEmpty());
    }

    @Test
    void tooShort_returnsError() {
        List<String> errors = validator.validate("Short1!", "testuser");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("at least 12"));
    }

    @Test
    void missingUppercase_returnsError() {
        List<String> errors = validator.validate("strongpass1!xy", "testuser");
        assertTrue(errors.stream().anyMatch(e -> e.contains("uppercase")));
    }

    @Test
    void missingLowercase_returnsError() {
        List<String> errors = validator.validate("STRONGPASS1!XY", "testuser");
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void missingDigit_returnsError() {
        List<String> errors = validator.validate("StrongPasswd!x", "testuser");
        assertTrue(errors.stream().anyMatch(e -> e.contains("digit")));
    }

    @Test
    void missingSpecial_returnsError() {
        List<String> errors = validator.validate("StrongPasswd1x", "testuser");
        assertTrue(errors.stream().anyMatch(e -> e.contains("non-alphanumeric")));
    }

    @Test
    void containsUsername_returnsError() {
        List<String> errors = validator.validate("MyTestuser1!xx", "testuser");
        assertTrue(errors.stream().anyMatch(e -> e.contains("username")));
    }

    @Test
    void shortUsername_noSubstringCheck() {
        // Username < 4 chars should not trigger substring check
        List<String> errors = validator.validate("StrongPass1!xy", "ab");
        assertTrue(errors.isEmpty());
    }

    @Test
    void nullPassword_returnsError() {
        List<String> errors = validator.validate(null, "testuser");
        assertFalse(errors.isEmpty());
    }

    @Test
    void exactMinLength_valid() {
        List<String> errors = validator.validate("Abcdefgh1!ab", "user");
        assertTrue(errors.isEmpty());
    }
}
