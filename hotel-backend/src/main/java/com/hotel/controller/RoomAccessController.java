package com.hotel.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotel.model.dto.RoomAccessVerifyRequest;
import com.hotel.model.dto.RoomAccessVerifyResponse;
import com.hotel.model.entity.RoomAccessLog;
import com.hotel.model.entity.RoomSecurityLockout;
import com.hotel.service.RoomAccessService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/access")
@RequiredArgsConstructor
public class RoomAccessController {

    private final RoomAccessService accessService;

    @PostMapping("/verify")
    public ResponseEntity<RoomAccessVerifyResponse> verifyAccess(@RequestBody RoomAccessVerifyRequest request) {
        RoomAccessVerifyResponse response = accessService.verifyAccess(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<RoomAccessLog>> getRecentLogs() {
        return ResponseEntity.ok(accessService.getRecentLogs());
    }

    @GetMapping("/status/{roomNumber}")
    public ResponseEntity<RoomSecurityLockout> getLockoutStatus(@PathVariable String roomNumber) {
        RoomSecurityLockout status = accessService.getLockoutStatus(roomNumber);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/reset/{roomNumber}")
    public ResponseEntity<Map<String, String>> resetLockout(@PathVariable String roomNumber) {
        accessService.resetLockout(roomNumber);
        return ResponseEntity.ok(Map.of("message", "房號 " + roomNumber + " 門鎖狀態已手動重設解除鎖定"));
    }
}

