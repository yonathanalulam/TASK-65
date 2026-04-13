package com.culinarycoach.security.auth;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.PasswordHistory;
import com.culinarycoach.domain.repository.PasswordHistoryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PasswordHistoryService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public PasswordHistoryService(PasswordHistoryRepository passwordHistoryRepository,
                                   PasswordEncoder passwordEncoder,
                                   AppProperties appProperties) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    public boolean isPasswordReused(Long userId, String rawPassword) {
        List<PasswordHistory> recent = passwordHistoryRepository.findRecentByUserId(userId);
        int historyCount = appProperties.getSecurity().getPasswordHistoryCount();
        return recent.stream()
            .limit(historyCount)
            .anyMatch(ph -> passwordEncoder.matches(rawPassword, ph.getPasswordHash()));
    }

    public void recordPassword(Long userId, String encodedPassword) {
        PasswordHistory entry = new PasswordHistory();
        entry.setUserId(userId);
        entry.setPasswordHash(encodedPassword);
        passwordHistoryRepository.save(entry);
    }
}
