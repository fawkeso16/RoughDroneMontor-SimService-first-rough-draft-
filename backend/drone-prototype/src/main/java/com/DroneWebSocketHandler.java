//web socket handler, we send 3 types of messages - paths, get drones and run sim.
//all messages are sychronized to prevent concurrent modification issues.
//we use a set to store sessions, this allows us to broadcast messages to all connected clients



package com;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DroneWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Object broadcastLock = new Object();

    private final DroneService droneService;

    @Autowired
    public DroneWebSocketHandler(DroneService testDroneService) {
        this.droneService = testDroneService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            if (session.isOpen()) {
                sendDronesToSession(session);
                sendPathsToSession(session);
                sessions.add(session);
            }
        } catch (IOException e) {
            System.err.println("Failed to send message to WebSocket client: " + e.getMessage());
            closeSessionSafely(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            var jsonNode = mapper.readTree(payload);
            String action = jsonNode.get("action").asText();
            switch (action) {
                case "getAll" -> sendDronesToSession(session);
                // case "moveAll" -> droneService.allJobs();
                default -> sendDronesToSession(session);
            }
        } catch (Exception e) {
            System.err.println("Error processing WebSocket message: {}" + e.getMessage());
            synchronized (broadcastLock) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Server error\"}"));
                    }
                } catch (IOException ioException) {
                    System.err.println("Failed to send error message: {}" + ioException.getMessage());
                }
            }
        }
    }

    private void sendDronesToSession(WebSocketSession session) throws IOException {
        synchronized (broadcastLock) {
            if (session.isOpen()) {
                var message = new WebSocketMessage("drones", droneService.getAllDrones());
                String json = mapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    private void sendPathsToSession(WebSocketSession session) throws IOException {
        synchronized (broadcastLock) {
            if (session.isOpen()) {
                var message = new WebSocketMessage("paths", droneService.getCurrentPaths());
                String json = mapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    @EventListener
    public void broadcastPaths(PathsUpdatedEvent event) {
        try {
            var message = new WebSocketMessage("paths", event.getAllPaths());
            broadcastMessage(message);
        } catch (Exception e) {
            System.err.println("WebSocket broadcast failed: " + e.getMessage());
        }
    }

    private void broadcastMessage(WebSocketMessage message) {
        synchronized (broadcastLock) {
            try {
                String json = mapper.writeValueAsString(message);
                TextMessage textMessage = new TextMessage(json);
                Set<WebSocketSession> sessionsCopy = Set.copyOf(sessions);
                for (WebSocketSession session : sessionsCopy) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            System.err.println("Failed to send to session: " + e.getMessage());
                            sessions.remove(session);
                        }
                    } else {
                        sessions.remove(session);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to serialize or broadcast message: " + e.getMessage());
            }
        }
    }

    private void closeSessionSafely(WebSocketSession session) {
        sessions.remove(session);
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException ex) {
                System.err.println("Failed to close WebSocket session: " + ex.getMessage());
            }
        }
    }

    public static class WebSocketMessage {
        public String type;
        public Object payload;

        public WebSocketMessage(String type, Object payload) {
            this.type = type;
            this.payload = payload;
        }
    }
}