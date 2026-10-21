package com.hotel.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_security_lockout")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSecurityLockout {

    @Id
    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures;

    @Column(name = "first_failure_time")
    private LocalDateTime firstFailureTime;

    @Column(name = "last_failure_time")
    private LocalDateTime lastFailureTime;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_unlocked_time")
    private LocalDateTime lastUnlockedTime;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

