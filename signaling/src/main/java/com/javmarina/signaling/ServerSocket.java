package com.javmarina.signaling;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;


@SuppressWarnings("unused")
@WebSocket(maxTextMessageSize = 64 * 1024)
public final class ServerSocket {

    private static final HashMap<Integer, RemoteEndpoint> serverSide = new HashMap<>(100);
    private static final HashMap<Integer, RemoteEndpoint> clientSide = new HashMap<>(100);

    private final Logger logger = Logger.getLogger(ServerSocket.class.getName());
    private Session session;
    private int id;
    private boolean isClient;

    @OnWebSocketConnect
    public void onWebSocketConnect(Session session) {
        this.session = session;
        logger.fine("Socket Connected: " + session);
    }

    @OnWebSocketMessage
    public void onWebSocketText(String message) {
        logger.fine("Received message: " + message);
        try {
            JSONObject jo = new JSONObject(message);
            switch (jo.getString("command")) {
                case "register-server":
                    final int tempId = jo.getInt("session-id");
                    if (serverSide.containsKey(tempId)) {
                        JSONObject response = new JSONObject();
                        response.put("command", "register-invalid");
                        this.session.getRemote().sendString(response.toString());
                    } else {
                        id = tempId;
                        isClient = false;
                        serverSide.put(id, session.getRemote());
                        JSONObject response = new JSONObject();
                        response.put("command", "register-ok");
                        this.session.getRemote().sendString(response.toString());
                    }
                    break;
                case "register-client":
                    final int tempId2 = jo.getInt("session-id");
                    if (clientSide.containsKey(tempId2) || !serverSide.containsKey(tempId2)) {
                        JSONObject response = new JSONObject();
                        response.put("command", "register-invalid");
                        this.session.getRemote().sendString(response.toString());
                    } else {
                        id = tempId2;
                        isClient = true;
                        clientSide.put(id, session.getRemote());
                        JSONObject response = new JSONObject();
                        response.put("command", "register-ok");
                        this.session.getRemote().sendString(response.toString());
                    }
                    break;
                case "offer":
                case "answer":
                case "candidate":
                    final HashMap<Integer, RemoteEndpoint> map = isClient ? serverSide : clientSide;
                    map.get(id).sendString(message);
                    break;
            }
        } catch (IOException e) {
            logger.severe("Error processing: " + e.getMessage());
        }
    }

    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason) {
        logger.fine("Socket Closed: [" + statusCode + "] " + reason);
        if (isClient) {
            clientSide.remove(id);
        } else {
            serverSide.remove(id);
        }
    }

    @OnWebSocketError
    public void onWebSocketError(Throwable cause) {
        logger.severe("Websocket error : " + cause.getMessage());
        if (isClient) {
            clientSide.remove(id);
        } else {
            serverSide.remove(id);
        }
    }
}