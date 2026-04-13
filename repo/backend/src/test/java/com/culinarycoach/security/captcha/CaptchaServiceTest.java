package com.culinarycoach.security.captcha;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.CaptchaChallenge;
import com.culinarycoach.domain.repository.CaptchaChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock private CaptchaChallengeRepository repository;

    private CaptchaService service;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder(4);
        AppProperties props = new AppProperties();
        props.getSecurity().setCaptchaExpiryMinutes(5);
        service = new CaptchaService(repository, encoder, props);
    }

    @Test
    void createChallenge_returnsIdAndImage() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        CaptchaService.CaptchaChallengeResult result = service.createChallenge("127.0.0.1");

        assertNotNull(result.challengeId());
        assertNotNull(result.imageBase64());
        assertTrue(result.imageBase64().startsWith("data:image/png;base64,"));
    }

    @Test
    void verifyCaptcha_correctAnswer() {
        String answer = "abcdef";
        CaptchaChallenge challenge = new CaptchaChallenge();
        challenge.setId("test-id");
        challenge.setAnswerHash(encoder.encode(answer.toLowerCase()));
        challenge.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        challenge.setUsed(false);

        when(repository.findById("test-id")).thenReturn(Optional.of(challenge));
        when(repository.save(any())).thenReturn(challenge);

        assertTrue(service.verifyCaptcha("test-id", answer));
        assertTrue(challenge.isUsed());
    }

    @Test
    void verifyCaptcha_wrongAnswer() {
        CaptchaChallenge challenge = new CaptchaChallenge();
        challenge.setId("test-id2");
        challenge.setAnswerHash(encoder.encode("correct"));
        challenge.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        challenge.setUsed(false);

        when(repository.findById("test-id2")).thenReturn(Optional.of(challenge));
        when(repository.save(any())).thenReturn(challenge);

        assertFalse(service.verifyCaptcha("test-id2", "wrong"));
    }

    @Test
    void verifyCaptcha_expired() {
        CaptchaChallenge challenge = new CaptchaChallenge();
        challenge.setId("expired-id");
        challenge.setAnswerHash(encoder.encode("answer"));
        challenge.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        challenge.setUsed(false);

        when(repository.findById("expired-id")).thenReturn(Optional.of(challenge));

        assertFalse(service.verifyCaptcha("expired-id", "answer"));
    }

    @Test
    void verifyCaptcha_alreadyUsed() {
        CaptchaChallenge challenge = new CaptchaChallenge();
        challenge.setId("used-id");
        challenge.setAnswerHash(encoder.encode("answer"));
        challenge.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        challenge.setUsed(true);

        when(repository.findById("used-id")).thenReturn(Optional.of(challenge));

        assertFalse(service.verifyCaptcha("used-id", "answer"));
    }

    @Test
    void verifyCaptcha_nullInputs() {
        assertFalse(service.verifyCaptcha(null, "answer"));
        assertFalse(service.verifyCaptcha("id", null));
    }
}
