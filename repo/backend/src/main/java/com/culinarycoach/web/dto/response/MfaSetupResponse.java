package com.culinarycoach.web.dto.response;

import java.util.List;

public record MfaSetupResponse(
    String qrCodeDataUri,
    String secretKey,
    List<String> recoveryCodes
) {}
