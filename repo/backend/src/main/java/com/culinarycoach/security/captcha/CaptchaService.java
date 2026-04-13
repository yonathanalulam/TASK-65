package com.culinarycoach.security.captcha;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.CaptchaChallenge;
import com.culinarycoach.domain.repository.CaptchaChallengeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class CaptchaService {

    private final CaptchaChallengeRepository captchaChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public CaptchaService(CaptchaChallengeRepository captchaChallengeRepository,
                           PasswordEncoder passwordEncoder,
                           AppProperties appProperties) {
        this.captchaChallengeRepository = captchaChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    public record CaptchaChallengeResult(String challengeId, String imageBase64) {}

    @Transactional
    public CaptchaChallengeResult createChallenge(String ipAddress) {
        CaptchaGenerator.CaptchaData data = CaptchaGenerator.generate();

        int expiryMinutes = appProperties.getSecurity().getCaptchaExpiryMinutes();

        CaptchaChallenge challenge = new CaptchaChallenge();
        challenge.setId(UUID.randomUUID().toString());
        challenge.setAnswerHash(passwordEncoder.encode(data.answer().toLowerCase()));
        challenge.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        challenge.setIpAddress(ipAddress);
        captchaChallengeRepository.save(challenge);

        return new CaptchaChallengeResult(challenge.getId(), data.imageBase64());
    }

    @Transactional
    public boolean verifyCaptcha(String challengeId, String answer) {
        if (challengeId == null || answer == null) return false;

        return captchaChallengeRepository.findById(challengeId)
            .filter(c -> !c.isUsed())
            .filter(c -> c.getExpiresAt().isAfter(Instant.now()))
            .map(c -> {
                c.setUsed(true);
                captchaChallengeRepository.save(c);
                return passwordEncoder.matches(answer.toLowerCase(), c.getAnswerHash());
            })
            .orElse(false);
    }

    @Scheduled(fixedRate = 600000) // every 10 minutes
    @Transactional
    public void cleanupExpiredChallenges() {
        captchaChallengeRepository.deleteExpiredChallenges(Instant.now());
    }
}
