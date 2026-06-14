package com.guessmelody.repository;

import com.guessmelody.model.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByRoomId(Long roomId);

    Optional<Player> findBySessionId(String sessionId);

    boolean existsBySessionIdAndRoomId(String sessionId, Long roomId);
}
