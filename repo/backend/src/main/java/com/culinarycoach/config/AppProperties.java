package com.culinarycoach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Security security = new Security();
    private RateLimit rateLimit = new RateLimit();
    private Audio audio = new Audio();

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

    public Audio getAudio() { return audio; }
    public void setAudio(Audio audio) { this.audio = audio; }

    public static class Security {
        private int bcryptStrength = 12;
        private int maxSessionsPerUser = 3;
        private int maxSessionsPerDevice = 2;
        private int idleTimeoutMinutes = 30;
        private int absoluteLifetimeHours = 12;
        private int lockoutDurationMinutes = 15;
        private int maxFailedAttempts = 5;
        private int failedAttemptWindowMinutes = 30;
        private int passwordMinLength = 12;
        private int passwordHistoryCount = 5;
        private int signatureValidityMinutes = 5;
        private int nonceCleanupIntervalMinutes = 10;
        private int captchaFailureThresholdLogin = 3;
        private int captchaFailureThresholdRequest = 10;
        private int captchaFailureWindowMinutes = 10;
        private int captchaExpiryMinutes = 5;
        private Mfa mfa = new Mfa();

        public int getBcryptStrength() { return bcryptStrength; }
        public void setBcryptStrength(int bcryptStrength) { this.bcryptStrength = bcryptStrength; }

        public int getMaxSessionsPerUser() { return maxSessionsPerUser; }
        public void setMaxSessionsPerUser(int v) { this.maxSessionsPerUser = v; }

        public int getMaxSessionsPerDevice() { return maxSessionsPerDevice; }
        public void setMaxSessionsPerDevice(int v) { this.maxSessionsPerDevice = v; }

        public int getIdleTimeoutMinutes() { return idleTimeoutMinutes; }
        public void setIdleTimeoutMinutes(int v) { this.idleTimeoutMinutes = v; }

        public int getAbsoluteLifetimeHours() { return absoluteLifetimeHours; }
        public void setAbsoluteLifetimeHours(int v) { this.absoluteLifetimeHours = v; }

        public int getLockoutDurationMinutes() { return lockoutDurationMinutes; }
        public void setLockoutDurationMinutes(int v) { this.lockoutDurationMinutes = v; }

        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int v) { this.maxFailedAttempts = v; }

        public int getFailedAttemptWindowMinutes() { return failedAttemptWindowMinutes; }
        public void setFailedAttemptWindowMinutes(int v) { this.failedAttemptWindowMinutes = v; }

        public int getPasswordMinLength() { return passwordMinLength; }
        public void setPasswordMinLength(int v) { this.passwordMinLength = v; }

        public int getPasswordHistoryCount() { return passwordHistoryCount; }
        public void setPasswordHistoryCount(int v) { this.passwordHistoryCount = v; }

        public int getSignatureValidityMinutes() { return signatureValidityMinutes; }
        public void setSignatureValidityMinutes(int v) { this.signatureValidityMinutes = v; }

        public int getNonceCleanupIntervalMinutes() { return nonceCleanupIntervalMinutes; }
        public void setNonceCleanupIntervalMinutes(int v) { this.nonceCleanupIntervalMinutes = v; }

        public int getCaptchaFailureThresholdLogin() { return captchaFailureThresholdLogin; }
        public void setCaptchaFailureThresholdLogin(int v) { this.captchaFailureThresholdLogin = v; }

        public int getCaptchaFailureThresholdRequest() { return captchaFailureThresholdRequest; }
        public void setCaptchaFailureThresholdRequest(int v) { this.captchaFailureThresholdRequest = v; }

        public int getCaptchaFailureWindowMinutes() { return captchaFailureWindowMinutes; }
        public void setCaptchaFailureWindowMinutes(int v) { this.captchaFailureWindowMinutes = v; }

        public int getCaptchaExpiryMinutes() { return captchaExpiryMinutes; }
        public void setCaptchaExpiryMinutes(int v) { this.captchaExpiryMinutes = v; }

        public Mfa getMfa() { return mfa; }
        public void setMfa(Mfa mfa) { this.mfa = mfa; }

        public static class Mfa {
            private String issuer = "CulinaryCoach";
            private String encryptionKey = "default-dev-key-change-in-production-32ch";

            public String getIssuer() { return issuer; }
            public void setIssuer(String issuer) { this.issuer = issuer; }

            public String getEncryptionKey() { return encryptionKey; }
            public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
        }
    }

    public static class Audio {
        private long cacheQuotaBytes = 2147483648L; // 2GB
        private int cacheValidityDays = 30;
        private long maxSegmentSizeBytes = 262144000L; // 250MB
        private String allowedAudioTypes = "mp3,aac,m4a,ogg";

        public long getCacheQuotaBytes() { return cacheQuotaBytes; }
        public void setCacheQuotaBytes(long v) { this.cacheQuotaBytes = v; }

        public int getCacheValidityDays() { return cacheValidityDays; }
        public void setCacheValidityDays(int v) { this.cacheValidityDays = v; }

        public long getMaxSegmentSizeBytes() { return maxSegmentSizeBytes; }
        public void setMaxSegmentSizeBytes(long v) { this.maxSegmentSizeBytes = v; }

        public String getAllowedAudioTypes() { return allowedAudioTypes; }
        public void setAllowedAudioTypes(String v) { this.allowedAudioTypes = v; }
    }

    public static class RateLimit {
        private int authenticatedPerMinute = 60;
        private int unauthenticatedPerMinute = 20;
        private int adminPerMinute = 30;
        private int burstCapacity = 10;
        private int burstPeriodSeconds = 2;

        public int getAuthenticatedPerMinute() { return authenticatedPerMinute; }
        public void setAuthenticatedPerMinute(int v) { this.authenticatedPerMinute = v; }

        public int getUnauthenticatedPerMinute() { return unauthenticatedPerMinute; }
        public void setUnauthenticatedPerMinute(int v) { this.unauthenticatedPerMinute = v; }

        public int getAdminPerMinute() { return adminPerMinute; }
        public void setAdminPerMinute(int v) { this.adminPerMinute = v; }

        public int getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(int v) { this.burstCapacity = v; }

        public int getBurstPeriodSeconds() { return burstPeriodSeconds; }
        public void setBurstPeriodSeconds(int v) { this.burstPeriodSeconds = v; }
    }
}
