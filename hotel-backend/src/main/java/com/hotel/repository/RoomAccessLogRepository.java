package com.hotel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hotel.model.entity.RoomAccessLog;

@Repository
public interface RoomAccessLogRepository extends JpaRepository<RoomAccessLog, Long> {

    List<RoomAccessLog> findByRoomNumberOrderByAttemptTimeDesc(String roomNumber);

    List<RoomAccessLog> findTop20ByOrderByAttemptTimeDesc();
}

