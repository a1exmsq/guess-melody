package com.guessmelody.repository;

import com.guessmelody.model.entity.Room;
import com.guessmelody.model.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    long countByStatus(RoomStatus status);
}
