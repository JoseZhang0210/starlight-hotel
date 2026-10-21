package com.hotel.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_access_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "card_id", nullable = false, length = 64)
    private String cardId;

    @Column(name = "access_result", nullable = false, length = 20)
    private String accessResult; // SUCCESS, FAILED, LOCKED

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(name = "device_id", length = 50)
    private String deviceId;
}

