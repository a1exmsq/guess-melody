package com.guessmelody.service;

import com.guessmelody.game.RoomState;
import com.guessmelody.repository.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameTrackPoolService trackPoolService;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameService gameService;

    @Test
    void shouldCreateRoomWithUniqueCode() {
        RoomState room = gameService.createRoom("Alice");

        assertThat(room).isNotNull();
        assertThat(room.getCode()).hasSize(5);
        assertThat(room.getHostName()).isEqualTo("Alice");
        assertThat(room.getStatus()).isEqualTo(RoomState.Status.WAITING);
    }

    @Test
    void shouldAllowPlayerToJoinExistingRoom() {
        RoomState room = gameService.createRoom("Alice");

        RoomState joinedRoom = gameService.joinRoom(room.getCode(), "Bob");

        assertThat(joinedRoom.getPlayers()).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void shouldThrowWhenJoiningNonExistingRoom() {
        assertThatThrownBy(() -> gameService.joinRoom("XXXXX", "Bob"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenNonHostStartsGame() {
        RoomState room = gameService.createRoom("Alice");
        gameService.joinRoom(room.getCode(), "Bob");

        assertThatThrownBy(() -> gameService.startGame(room.getCode(), "Bob"))
                .isInstanceOf(IllegalStateException.class);
    }
}
