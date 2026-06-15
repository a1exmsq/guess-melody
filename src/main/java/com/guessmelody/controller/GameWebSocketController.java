package com.guessmelody.controller;

import com.guessmelody.dto.GameMessage;
import com.guessmelody.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{code}/join")
    public void joinRoom(@DestinationVariable String code, @Payload GameMessage msg) {
        try {
            String playerName = msg.getPlayerName();
            var room = gameService.joinRoom(code, playerName);

            broadcast(code, GameMessage.builder()
                    .type(GameMessage.Type.PLAYER_JOINED)
                    .roomCode(code)
                    .playerName(playerName)
                    .message(playerName + " joined!")
                    .players(new java.util.ArrayList<>(room.getPlayers()))
                    .scores(new java.util.HashMap<>(room.getScores()))
                    .build());

        } catch (Exception e) {
            log.error("Join error room {}", code, e);
            broadcastError(code, e.getMessage());
        }
    }

    @MessageMapping("/room/{code}/leave")
    public void leaveRoom(@DestinationVariable String code, @Payload GameMessage msg) {
        try {
            gameService.leaveRoom(code, msg.getPlayerName());
        } catch (Exception e) {
            log.error("Leave error room {}", code, e);
            broadcastError(code, e.getMessage());
        }
    }

    @MessageMapping("/room/{code}/start")
    public void startGame(@DestinationVariable String code, @Payload GameMessage msg) {
        try {
            gameService.startGame(code, msg.getPlayerName());
        } catch (Exception e) {
            log.error("Start game error room {}", code, e);
            broadcastError(code, e.getMessage());
        }
    }

    @MessageMapping("/room/{code}/play")
    public void playTrack(@DestinationVariable String code, @Payload GameMessage msg) {
        try {
            gameService.playTrackRequest(code, msg.getPlayerName());
        } catch (Exception e) {
            log.error("Play track error room {}", code, e);
            broadcastError(code, e.getMessage());
        }
    }

    @MessageMapping("/room/{code}/guess")
    public void guess(@DestinationVariable String code, @Payload GameMessage msg) {
        try {
            gameService.submitGuess(code, msg.getPlayerName(), msg.getMessage(), msg.getAttempt());
        } catch (Exception e) {
            log.error("Guess error room {}", code, e);
            broadcastError(code, e.getMessage());
        }
    }

    @MessageMapping("/room/{code}/end-round")
    public void endRound(@DestinationVariable String code, @Payload GameMessage msg) {
        try {
            var room = gameService.getRoom(code);
            if (room != null && room.isHost(msg.getPlayerName())) {
                gameService.endRound(code);
            }
        } catch (Exception e) {
            log.error("End round error room {}", code, e);
        }
    }

    private void broadcast(String roomCode, GameMessage msg) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(), msg);
    }

    private void broadcastError(String roomCode, String error) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(),
                GameMessage.builder()
                        .type(GameMessage.Type.ERROR)
                        .message(error)
                        .build());
    }
}
