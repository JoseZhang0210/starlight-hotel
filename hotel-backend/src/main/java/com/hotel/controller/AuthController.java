package com.hotel.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.hotel.model.dto.MemberDTO;
import com.hotel.model.entity.Account;
import com.hotel.model.entity.Profile;
import com.hotel.repository.AccountRepository;
import com.hotel.repository.ProfileRepository;
import com.hotel.service.MemberService;
import com.hotel.util.JsonUtils;
import com.hotel.util.JwtUtils;
import com.hotel.util.MailUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final ProfileRepository profileRepository;
    private final MemberService memberService;
    private final MailUtil mailUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    private final RestClient restClient = RestClient.create();

    // Google Token 驗證結果
    private record GoogleUserInfo(String email, String name, String avatarUrl) {}

    // 儲存 Email 與驗證碼資訊 (驗證碼、過期時間)
    private static class VerificationCodeRecord {
        final String code;
        final long expireTime;

        VerificationCodeRecord(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    private final Map<String, VerificationCodeRecord> verificationCodes = new ConcurrentHashMap<>();
    private final Map<String, VerificationCodeRecord> resetPasswordCodes = new ConcurrentHashMap<>();

    // =====================================================
    // 檢查帳號是否重複
    // GET /api/auth/check-username?username=xxx
    // =====================================================
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam(name = "username", required = false) String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("exists", false, "message", "帳號不可為空"));
        }
        boolean exists = accountRepository.existsByUsername(username.trim());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // =====================================================
    // 檢查電子信箱是否重複
    // GET /api/auth/check-email?email=xxx
    // =====================================================
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam(name = "email", required = false) String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("exists", false, "message", "電子信箱不可為空"));
        }
        boolean exists = profileRepository.existsByEmailIgnoreCase(email.trim());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // =====================================================
    // 發送信箱驗證碼
    // POST /api/auth/send-code
    // =====================================================
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("message", "請輸入有效的電子信箱"));
        }
        email = email.trim().toLowerCase();

        // 產生 6 位數隨機數字
        int randomCode = 100000 + (int) (Math.random() * 900000);
        String code = String.valueOf(randomCode);

        // 有效期 5 分鐘
        long expireTime = System.currentTimeMillis() + (5 * 60 * 1000);
        verificationCodes.put(email, new VerificationCodeRecord(code, expireTime));

        try {
            mailUtil.sendVerificationCode(email, code);
            return ResponseEntity.ok(Map.of("message", "驗證碼已發送至您的信箱，請於 5 分鐘內輸入"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "發送驗證碼失敗，請稍後再試"));
        }
    }

    // =====================================================
    // 會員註冊 (含個人資料與信箱驗證碼校驗)
    // POST /api/auth/register
    // =====================================================
    @PostMapping("/register")
    public ResponseEntity<?> registerMember(@RequestBody MemberDTO memberDTO) {
        try {
            // 基本欄位校驗
            if (memberDTO.getUsername() == null || memberDTO.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "請輸入帳號"));
            }
            if (memberDTO.getPassword() == null || memberDTO.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "請輸入密碼"));
            }
            if (memberDTO.getEmail() == null || memberDTO.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "請輸入電子信箱"));
            }

            // 信箱驗證碼校驗
            String email = memberDTO.getEmail().trim().toLowerCase();
            if (profileRepository.existsByEmailIgnoreCase(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "此電子信箱已被註冊"));
            }
            String inputCode = memberDTO.getVerificationCode();
            if (inputCode == null || inputCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "請輸入信箱驗證碼"));
            }

            VerificationCodeRecord record = verificationCodes.get(email);
            if (record == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "尚未向該信箱發送驗證碼，請點擊「發送驗證碼」"));
            }
            if (record.isExpired()) {
                verificationCodes.remove(email);
                return ResponseEntity.badRequest().body(Map.of("message", "驗證碼已過期，請重新發送"));
            }
            if (!record.code.equalsIgnoreCase(inputCode.trim())) {
                return ResponseEntity.badRequest().body(Map.of("message", "驗證碼不正確，請重新確認"));
            }

            // 驗證碼正確，移除快取以防重複使用
            verificationCodes.remove(email);

            // 預設狀態設為啟用 "1"
            memberDTO.setStatus("1");

            // 建立完整會員資料 (Account + Member + Profile)
            MemberDTO createdMember = memberService.createMember(memberDTO);

            // 註冊成功後自動生成 JWT Token 與登入資訊
            UserDetails user = userDetailsService.loadUserByUsername(createdMember.getUsername());
            String token = jwtUtils.generateToken(user);

            java.util.List<String> authorities = user.getAuthorities().stream()
                    .map(auth -> auth != null ? auth.getAuthority() : null)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            String displayName = (createdMember.getName() != null && !createdMember.getName().isBlank())
                    ? createdMember.getName()
                    : createdMember.getUsername();

            Map<String, Object> response = new HashMap<>();
            response.put("member", createdMember);
            response.put("token", token);
            response.put("authorities", authorities);
            response.put("name", displayName);
            response.put("avatarUrl", createdMember.getAvatarUrl());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "此帳號已被註冊"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "註冊失敗：" + e.getMessage()));
        }
    }

    // =====================================================
    // 登入
    // POST /api/auth/login
    // =====================================================
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Account account) {
        UserDetails user = userDetailsService.loadUserByUsername(account.getUsername());

        if (!passwordEncoder.matches(account.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Authentication fails because of incorrect password.");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled.");
        }

        String token = jwtUtils.generateToken(user);

        java.util.List<String> authorities = user.getAuthorities().stream()
                .map(auth -> auth != null ? auth.getAuthority() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        Profile userProfile = profileRepository.findByUsername(user.getUsername()).orElse(null);
        String name = (userProfile != null && userProfile.getName() != null && !userProfile.getName().isBlank())
                ? userProfile.getName()
                : user.getUsername();
        String avatarUrl = userProfile != null ? userProfile.getAvatarUrl() : null;

        response.put("token", token);
        response.put("authorities", authorities);
        response.put("name", name);
        response.put("avatarUrl", avatarUrl);
        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Google 第三方登入
    // POST /api/auth/google-login
    // =====================================================
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String credential = request.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未提供有效的 Google 授權憑證"));
        }

        // 後端嚴格校驗 Google ID Token (簽章、過期時間、受眾 aud 與 email_verified)
        GoogleUserInfo googleUser = verifyGoogleToken(credential);
        if (googleUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Google 授權憑證無效、過期或簽名驗證失敗"));
        }

        String email = googleUser.email();
        String name = googleUser.name();
        String avatarUrl = googleUser.avatarUrl();

        // 查詢是否已存在以此 Email 註冊的會員
        Profile profile = profileRepository.findFirstByEmail(email).orElse(null);

        if (profile != null && profile.getAccountId() != null) {
            // 已註冊：執行登入並發放 JWT
            Account account = accountRepository.findById(profile.getAccountId()).orElse(null);
            if (account == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "查無對應的會員帳號資料"));
            }

            if ("0".equals(account.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "該帳號已被停用，請聯絡客服人員"));
            }

            UserDetails user = userDetailsService.loadUserByUsername(account.getUsername());
            String token = jwtUtils.generateToken(user);

            java.util.List<String> authorities = user.getAuthorities().stream()
                    .map(auth -> auth != null ? auth.getAuthority() : null)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            String displayName = (profile.getName() != null && !profile.getName().isBlank())
                    ? profile.getName()
                    : user.getUsername();

            Map<String, Object> response = new HashMap<>();
            response.put("registered", true);
            response.put("token", token);
            response.put("authorities", authorities);
            response.put("name", displayName);
            response.put("avatarUrl", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : avatarUrl);
            response.put("username", user.getUsername());
            return ResponseEntity.ok(response);
        } else {
            // 尚未註冊：生成專屬 Google 預先認證碼並存入快取（15 分鐘效期）
            String googleVerifiedCode = "G-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            long expireTime = System.currentTimeMillis() + (15 * 60 * 1000);
            verificationCodes.put(email, new VerificationCodeRecord(googleVerifiedCode, expireTime));

            Map<String, Object> response = new HashMap<>();
            response.put("registered", false);
            response.put("email", email);
            response.put("name", name != null ? name : "");
            response.put("avatarUrl", avatarUrl != null ? avatarUrl : "");
            response.put("googleVerifiedCode", googleVerifiedCode);
            response.put("message", "此 Google 帳號尚未註冊，即將為您引導至註冊頁面並帶入資料");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 呼叫 Google 官方 tokeninfo 端點校驗 ID Token 簽章、發行者、受眾與 email_verified
     */
    private GoogleUserInfo verifyGoogleToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return null;
        }
        try {
            String responseBody = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> payload = JsonUtils.toMap(responseBody);
            if (payload == null) {
                return null;
            }

            // 1. 驗證 issuer
            String iss = String.valueOf(payload.get("iss"));
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                return null;
            }

            // 2. 驗證 audience / azp 符合本系統 Client ID
            String aud = String.valueOf(payload.get("aud"));
            String azp = String.valueOf(payload.get("azp"));
            if (!googleClientId.equals(aud) && !googleClientId.equals(azp)) {
                return null;
            }

            // 3. 驗證信箱通過 Google 認證
            Object emailVerified = payload.get("email_verified");
            if (!"true".equalsIgnoreCase(String.valueOf(emailVerified))) {
                return null;
            }

            // 4. 提取真實使用者資訊
            String email = String.valueOf(payload.get("email")).trim().toLowerCase();
            if (email.isEmpty() || !email.contains("@")) {
                return null;
            }

            String name = payload.get("name") != null ? String.valueOf(payload.get("name")) : "";
            String picture = payload.get("picture") != null ? String.valueOf(payload.get("picture")) : "";

            return new GoogleUserInfo(email, name, picture);
        } catch (Exception e) {
            // Google API 回應非 200 (Token 無效或過期) 或連線異常
            return null;
        }
    }

    // =====================================================
    // 刷新 Token
    // POST /api/auth/refresh
    // =====================================================
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未提供有效的 Authorization Header"));
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token 已失效或過期，請重新登入"));
        }

        try {
            String username = jwtUtils.extractUsername(token);
            UserDetails user = userDetailsService.loadUserByUsername(username);

            if (!user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "該帳號已被停用"));
            }

            String newToken = jwtUtils.generateToken(user);

            java.util.List<String> authorities = user.getAuthorities().stream()
                    .map(auth -> auth != null ? auth.getAuthority() : null)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            Profile userProfile = profileRepository.findByUsername(user.getUsername()).orElse(null);
            String name = (userProfile != null && userProfile.getName() != null && !userProfile.getName().isBlank())
                    ? userProfile.getName()
                    : user.getUsername();
            String avatarUrl = userProfile != null ? userProfile.getAvatarUrl() : null;

            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("authorities", authorities);
            response.put("name", name);
            response.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token 刷新失敗：" + e.getMessage()));
        }
    }

    // =====================================================
    // 忘記密碼 - 發送驗證碼
    // POST /api/auth/forgot-password/send-code
    // =====================================================
    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<?> sendForgotPasswordCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String username = request.get("username");

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("message", "請輸入有效的電子信箱"));
        }
        email = email.trim().toLowerCase();

        // 檢查信箱對應的使用者是否存在
        Profile profile;
        if (username != null && !username.trim().isEmpty()) {
            profile = profileRepository.findByUsernameAndEmail(username.trim(), email).orElse(null);
        } else {
            profile = profileRepository.findFirstByEmail(email).orElse(null);
        }

        if (profile == null || profile.getAccountId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "查無此電子信箱註冊的會員帳號"));
        }

        Account account = accountRepository.findById(profile.getAccountId()).orElse(null);
        if (account == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "查無對應的會員帳號資訊"));
        }

        if ("0".equals(account.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "該帳號已被停用，請聯絡客服人員"));
        }

        // 產生 6 位數隨機驗證碼
        int randomCode = 100000 + (int) (Math.random() * 900000);
        String code = String.valueOf(randomCode);

        // 有效期 5 分鐘
        long expireTime = System.currentTimeMillis() + (5 * 60 * 1000);
        resetPasswordCodes.put(email, new VerificationCodeRecord(code, expireTime));

        try {
            mailUtil.sendResetPasswordCode(email, code);
            return ResponseEntity.ok(Map.of("message", "重設密碼驗證碼已發送至您的信箱，請於 5 分鐘內輸入"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "發送驗證碼失敗，請稍後再試"));
        }
    }

    // =====================================================
    // 忘記密碼 - 重設密碼
    // POST /api/auth/forgot-password/reset
    // =====================================================
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String username = request.get("username");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("message", "請輸入有效的電子信箱"));
        }
        email = email.trim().toLowerCase();

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "請輸入 6 位數驗證碼"));
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "新密碼長度至少需為 6 個字元"));
        }

        // 檢查驗證碼
        VerificationCodeRecord record = resetPasswordCodes.get(email);
        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "尚未向該信箱發送重設驗證碼，請先點擊「發送驗證碼」"));
        }
        if (record.isExpired()) {
            resetPasswordCodes.remove(email);
            return ResponseEntity.badRequest().body(Map.of("message", "驗證碼已過期，請重新發送"));
        }
        if (!record.code.equalsIgnoreCase(code.trim())) {
            return ResponseEntity.badRequest().body(Map.of("message", "驗證碼不正確，請重新確認"));
        }

        // 查找使用者並更新密碼
        Profile profile;
        if (username != null && !username.trim().isEmpty()) {
            profile = profileRepository.findByUsernameAndEmail(username.trim(), email).orElse(null);
        } else {
            profile = profileRepository.findFirstByEmail(email).orElse(null);
        }

        if (profile == null || profile.getAccountId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "查無此會員帳號"));
        }

        Account account = accountRepository.findById(profile.getAccountId()).orElse(null);
        if (account == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "查無此會員帳號"));
        }

        account.setPassword(passwordEncoder.encode(newPassword.trim()));
        accountRepository.save(account);

        // 重設成功，移除快取驗證碼
        resetPasswordCodes.remove(email);

        return ResponseEntity.ok(Map.of("message", "密碼重設成功，請使用新密碼登入"));
    }
}

