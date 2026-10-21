package com.hotel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAccessVerifyResponse {
    private String status; // "GRANTED", "DENIED", "LOCKED", "LOCKOUT_TRIGGERED"
    private String message;
    private Integer remainingLockSeconds;
    private Integer remainingAttempts;
    private String roomNumber;
    private String cardId;
}

