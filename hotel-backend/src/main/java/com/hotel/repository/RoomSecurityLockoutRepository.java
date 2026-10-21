package com.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hotel.model.entity.RoomSecurityLockout;

@Repository
public interface RoomSecurityLockoutRepository extends JpaRepository<RoomSecurityLockout, String> {
}

