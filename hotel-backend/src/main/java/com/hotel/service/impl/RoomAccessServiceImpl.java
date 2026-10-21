package com.hotel.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotel.model.dto.RoomAccessVerifyRequest;
import com.hotel.model.dto.RoomAccessVerifyResponse;
import com.hotel.model.entity.RoomAccessLog;
import com.hotel.model.entity.RoomSecurityLockout;
import com.hotel.repository.RoomAccessLogRepository;
import com.hotel.repository.RoomSecurityLockoutRepository;
import com.hotel.service.RoomAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomAccessServiceImpl implements RoomAccessService {

    private final RoomAccessLogRepository logRepository;
    private final RoomSecurityLockoutRepository lockoutRepository;

    @Override
    @Transactional
    public RoomAccessVerifyResponse verifyAccess(RoomAccessVerifyRequest request) {
        String roomNumber = request.getRoomNumber();
        String cardId = request.getCardId();
        boolean isValidCard = Boolean.TRUE.equals(request.getIsValidCard());
        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "ESP32_READER_01";
        LocalDateTime now = LocalDateTime.now();

        // 1. 取得或初始化該房門安全狀態
        RoomSecurityLockout state = lockoutRepository.findById(roomNumber).orElseGet(() -> {
            RoomSecurityLockout newState = RoomSecurityLockout.builder()
                    .roomNumber(roomNumber)
                    .consecutiveFailures(0)
                    .isLocked(false)
                    .updatedAt(now)
                    .build();
            return lockoutRepository.save(newState);
        });

        // 2. 檢查是否處於 5 分鐘冷卻鎖定中
        if (Boolean.TRUE.equals(state.getIsLocked())) {
            if (state.getLockedUntil() != null && now.isBefore(state.getLockedUntil())) {
                long remainingSeconds = ChronoUnit.SECONDS.between(now, state.getLockedUntil());

                // 寫入拒絕日誌
                logRepository.save(RoomAccessLog.builder()
                        .roomNumber(roomNumber)
                        .cardId(cardId)
                        .accessResult("LOCKED")
                        .failureReason("門鎖防禦鎖定中（5分鐘冷卻）")
                        .attemptTime(now)
                        .deviceId(deviceId)
                        .build());

                return RoomAccessVerifyResponse.builder()
                        .status("LOCKED")
                        .message("🚨 3分鐘內失敗超過5次，門鎖強制鎖定5分鐘！請等待 " + remainingSeconds + " 秒後再試")
                        .remainingLockSeconds((int) remainingSeconds)
                        .remainingAttempts(0)
                        .roomNumber(roomNumber)
                        .cardId(cardId)
                        .build();
            } else {
                // 鎖定已過期，自動解除
                state.setIsLocked(false);
                state.setConsecutiveFailures(0);
                state.setLockedUntil(null);
                state.setFirstFailureTime(null);
            }
        }

        // 3. 判斷卡片合法性
        if (isValidCard) {
            // 開門成功：重設失敗次數
            state.setConsecutiveFailures(0);
            state.setFirstFailureTime(null);
            state.setLastUnlockedTime(now);
            state.setUpdatedAt(now);
            lockoutRepository.save(state);

            // 記錄成功日誌
            logRepository.save(RoomAccessLog.builder()
                    .roomNumber(roomNumber)
                    .cardId(cardId)
                    .accessResult("SUCCESS")
                    .failureReason(null)
                    .attemptTime(now)
                    .deviceId(deviceId)
                    .build());

            return RoomAccessVerifyResponse.builder()
                    .status("GRANTED")
                    .message("驗證成功！房號 " + roomNumber + " 門鎖已開啟")
                    .remainingLockSeconds(0)
                    .remainingAttempts(5)
                    .roomNumber(roomNumber)
                    .cardId(cardId)
                    .build();
        } else {
            // 驗證失敗：累計 3 分鐘窗口內之失敗次數
            LocalDateTime firstFail = state.getFirstFailureTime();
            int currentFailures = state.getConsecutiveFailures() != null ? state.getConsecutiveFailures() : 0;

            if (firstFail == null || ChronoUnit.SECONDS.between(firstFail, now) > 180) {
                // 超過 3 分鐘，重新開始計算窗口
                state.setFirstFailureTime(now);
                state.setConsecutiveFailures(1);
            } else {
                state.setConsecutiveFailures(currentFailures + 1);
            }

            state.setLastFailureTime(now);
            state.setUpdatedAt(now);

            // 檢查是否超過 5 次 (即失敗達第 6 次，或依條件超過 5 次)
            if (state.getConsecutiveFailures() > 5) {
                // 觸發 5 分鐘強制鎖定
                LocalDateTime lockUntil = now.plusMinutes(5);
                state.setIsLocked(true);
                state.setLockedUntil(lockUntil);
                lockoutRepository.save(state);

                logRepository.save(RoomAccessLog.builder()
                        .roomNumber(roomNumber)
                        .cardId(cardId)
                        .accessResult("LOCKED")
                        .failureReason("3分鐘內失敗超過5次，觸發5分鐘強制鎖定")
                        .attemptTime(now)
                        .deviceId(deviceId)
                        .build());

                return RoomAccessVerifyResponse.builder()
                        .status("LOCKOUT_TRIGGERED")
                        .message("🚨 3分鐘內失敗超過5次，門鎖強制鎖定5分鐘！")
                        .remainingLockSeconds(300)
                        .remainingAttempts(0)
                        .roomNumber(roomNumber)
                        .cardId(cardId)
                        .build();
            } else {
                lockoutRepository.save(state);

                int remainingAttempts = Math.max(0, 5 - state.getConsecutiveFailures() + 1);

                logRepository.save(RoomAccessLog.builder()
                        .roomNumber(roomNumber)
                        .cardId(cardId)
                        .accessResult("FAILED")
                        .failureReason("未授權卡片或卡號不符")
                        .attemptTime(now)
                        .deviceId(deviceId)
                        .build());

                return RoomAccessVerifyResponse.builder()
                        .status("DENIED")
                        .message("卡號未授權！3分鐘內剩餘可嘗試次數：" + remainingAttempts)
                        .remainingLockSeconds(0)
                        .remainingAttempts(remainingAttempts)
                        .roomNumber(roomNumber)
                        .cardId(cardId)
                        .build();
            }
        }
    }

    @Override
    public List<RoomAccessLog> getRecentLogs() {
        return logRepository.findTop20ByOrderByAttemptTimeDesc();
    }

    @Override
    public RoomSecurityLockout getLockoutStatus(String roomNumber) {
        return lockoutRepository.findById(roomNumber).orElse(null);
    }

    @Override
    @Transactional
    public void resetLockout(String roomNumber) {
        lockoutRepository.findById(roomNumber).ifPresent(state -> {
            state.setIsLocked(false);
            state.setConsecutiveFailures(0);
            state.setLockedUntil(null);
            state.setFirstFailureTime(null);
            state.setUpdatedAt(LocalDateTime.now());
            lockoutRepository.save(state);
        });
    }
}

