package com.guessmelody.websocket;

import com.guessmelody.dto.GameMessage;
import com.guessmelody.model.entity.Track;
import com.guessmelody.service.GameTrackPoolService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @MockBean
    private GameTrackPoolService trackPoolService;

    private MappingJackson2MessageConverter messageConverter;

    private WebSocketStompClient stompClient;
    private StompSession session;
    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setUp() throws Exception {
        messageConverter = new MappingJackson2MessageConverter();
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(messageConverter);

        Track track = Track.builder()
                .spotifyTrackId("track1")
                .name("Test Track")
                .artistName("Test Artist")
                .previewUrl("https://example.com/preview.mp3")
                .build();
        when(trackPoolService.getAllTracks()).thenReturn(List.of(track));

        session = stompClient.connect(
                "ws://localhost:" + port + "/ws/websocket",
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        stompClient.stop();
    }

    @Test
    void playerShouldReceiveJoinAndStartMessages() throws Exception {
        String roomCode = createRoomViaRest("Alice");

        BlockingQueue<GameMessage> received = new LinkedBlockingQueue<>();
        subscribe(session, roomCode, received);

        send(session, "/app/room/" + roomCode + "/join", joinMessage("Bob"));

        GameMessage joined = received.poll(5, TimeUnit.SECONDS);
        assertThat(joined).isNotNull();
        assertThat(joined.getType()).isEqualTo(GameMessage.Type.PLAYER_JOINED);
        assertThat(joined.getPlayers()).contains("Alice", "Bob");

        send(session, "/app/room/" + roomCode + "/start", hostMessage("Alice"));

        GameMessage started = received.poll(5, TimeUnit.SECONDS);
        assertThat(started).isNotNull();
        assertThat(started.getType()).isEqualTo(GameMessage.Type.GAME_STARTED);
    }

    @SuppressWarnings("unchecked")
    private String createRoomViaRest(String hostName) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/rooms",
                Map.of("playerName", hostName),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return (String) response.getBody().get("code");
    }

    private void subscribe(StompSession session, String roomCode, BlockingQueue<GameMessage> queue) {
        session.subscribe("/topic/room/" + roomCode, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return GameMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((GameMessage) payload);
            }
        });
    }

    private void send(StompSession session, String destination, GameMessage message) {
        session.send(destination, message);
    }

    private GameMessage joinMessage(String playerName) {
        GameMessage msg = new GameMessage();
        msg.setPlayerName(playerName);
        return msg;
    }

    private GameMessage hostMessage(String playerName) {
        GameMessage msg = new GameMessage();
        msg.setPlayerName(playerName);
        return msg;
    }
}
