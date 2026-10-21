package com.hotel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomAccessVerifyRequest {
    private String roomNumber;
    private String cardId;
    private Boolean isValidCard;
    private String deviceId;
}

