package com.culinarycoach.security.mfa;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.MfaSecret;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.repository.MfaSecretRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.web.dto.response.MfaSetupResponse;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TotpService {

    private final MfaSecretRepository mfaSecretRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public TotpService(MfaSecretRepository mfaSecretRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AppProperties appProperties) {
        this.mfaSecretRepository = mfaSecretRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Transactional
    public MfaSetupResponse setupMfa(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isMfaEnabled()) {
            throw new IllegalStateException("MFA is already enabled");
        }

        // Delete any existing unverified secret
        mfaSecretRepository.findByUserId(userId).ifPresent(s -> mfaSecretRepository.delete(s));

        // Generate secret
        String secret = new DefaultSecretGenerator().generate();
        String encryptionKey = appProperties.getSecurity().getMfa().getEncryptionKey();
        String encryptedSecret = EncryptionUtil.encrypt(secret, encryptionKey);

        // Generate recovery codes
        String[] codes = new RecoveryCodeGenerator().generateCodes(8);
        List<String> plainCodes = Arrays.asList(codes);

        // Hash recovery codes for storage
        String hashedCodes = plainCodes.stream()
            .map(passwordEncoder::encode)
            .collect(Collectors.joining(","));

        // Store
        MfaSecret mfaSecret = new MfaSecret();
        mfaSecret.setUserId(userId);
        mfaSecret.setEncryptedSecret(encryptedSecret);
        mfaSecret.setVerified(false);
        mfaSecret.setRecoveryCodes(hashedCodes);
        mfaSecretRepository.save(mfaSecret);

        // Generate QR code
        String issuer = appProperties.getSecurity().getMfa().getIssuer();
        QrData qrData = new QrData.Builder()
            .label(user.getUsername())
            .secret(secret)
            .issuer(issuer)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

        String qrCodeDataUri;
        try {
            ZxingPngQrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(qrData);
            qrCodeDataUri = getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }

        return new MfaSetupResponse(qrCodeDataUri, secret, plainCodes);
    }

    @Transactional
    public boolean verifyAndEnable(Long userId, String code) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("No MFA setup in progress"));

        String encryptionKey = appProperties.getSecurity().getMfa().getEncryptionKey();
        String secret = EncryptionUtil.decrypt(mfaSecret.getEncryptedSecret(), encryptionKey);

        if (!verifyCode(secret, code)) {
            return false;
        }

        mfaSecret.setVerified(true);
        mfaSecretRepository.save(mfaSecret);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setMfaEnabled(true);
        userRepository.save(user);

        return true;
    }

    public boolean verifyCode(Long userId, String code) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("MFA not configured"));

        if (!mfaSecret.isVerified()) {
            throw new IllegalStateException("MFA setup not completed");
        }

        String encryptionKey = appProperties.getSecurity().getMfa().getEncryptionKey();
        String secret = EncryptionUtil.decrypt(mfaSecret.getEncryptedSecret(), encryptionKey);

        return verifyCode(secret, code);
    }

    public boolean verifyRecoveryCode(Long userId, String recoveryCode) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("MFA not configured"));

        String[] hashedCodes = mfaSecret.getRecoveryCodes().split(",");
        List<String> remaining = new ArrayList<>();
        boolean found = false;

        for (String hashed : hashedCodes) {
            if (!found && passwordEncoder.matches(recoveryCode, hashed)) {
                found = true; // Consume this code
            } else {
                remaining.add(hashed);
            }
        }

        if (found) {
            mfaSecret.setRecoveryCodes(String.join(",", remaining));
            mfaSecretRepository.save(mfaSecret);
        }

        return found;
    }

    @Transactional
    public void disableMfa(Long userId) {
        mfaSecretRepository.deleteByUserId(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setMfaEnabled(false);
        userRepository.save(user);
    }

    private boolean verifyCode(String secret, String code) {
        CodeVerifier verifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(), new SystemTimeProvider());
        return verifier.isValidCode(secret, code);
    }
}
