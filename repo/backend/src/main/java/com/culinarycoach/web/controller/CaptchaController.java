package com.culinarycoach.web.controller;

import com.culinarycoach.security.captcha.CaptchaService;
import com.culinarycoach.web.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/challenge")
    public ResponseEntity<ApiResponse<Map<String, String>>> getChallenge(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        CaptchaService.CaptchaChallengeResult result = captchaService.createChallenge(ip);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "challengeId", result.challengeId(),
            "image", result.imageBase64()
        )));
    }
}
