package com.hotel.service;

import java.util.List;

import com.hotel.model.dto.RoomAccessVerifyRequest;
import com.hotel.model.dto.RoomAccessVerifyResponse;
import com.hotel.model.entity.RoomAccessLog;
import com.hotel.model.entity.RoomSecurityLockout;

public interface RoomAccessService {

    RoomAccessVerifyResponse verifyAccess(RoomAccessVerifyRequest request);

    List<RoomAccessLog> getRecentLogs();

    RoomSecurityLockout getLockoutStatus(String roomNumber);

    void resetLockout(String roomNumber);
}

