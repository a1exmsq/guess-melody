package com.guessmelody.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Persisted player metadata for a room.
 */
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** WebSocket session id of the player. */
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "score")
    @Builder.Default
    private Integer score = 0;

    @Column(name = "is_host")
    @Builder.Default
    private Boolean isHost = false;

    @Column(name = "is_connected")
    @Builder.Default
    private Boolean isConnected = true;
}
