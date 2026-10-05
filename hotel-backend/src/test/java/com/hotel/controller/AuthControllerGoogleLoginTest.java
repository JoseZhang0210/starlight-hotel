package com.hotel.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hotel.repository.AccountRepository;
import com.hotel.repository.ProfileRepository;
import com.hotel.service.MemberService;
import com.hotel.util.JwtUtils;
import com.hotel.util.MailUtil;

class AuthControllerGoogleLoginTest {

    private AuthController authController;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtUtils jwtUtils = mock(JwtUtils.class);
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        MemberService memberService = mock(MemberService.class);
        MailUtil mailUtil = mock(MailUtil.class);

        authController = new AuthController(
                passwordEncoder,
                accountRepository,
                userDetailsService,
                jwtUtils,
                profileRepository,
                memberService,
                mailUtil
        );
    }

    @Test
    void testGoogleLoginWithoutCredentialIsRejected() {
        // 攻擊測試：攻擊者僅送出受害者 email，企圖繞過驗證取得 Token
        Map<String, String> request = Map.of("email", "admin@starlight.com");
        ResponseEntity<?> response = authController.googleLogin(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGoogleLoginWithInvalidCredentialIsRejected() {
        // 攻擊測試：送出偽造未授權的 credential
        Map<String, String> request = Map.of(
                "credential", "fake.jwt.token",
                "email", "victim@starlight.com"
        );
        ResponseEntity<?> response = authController.googleLogin(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}

