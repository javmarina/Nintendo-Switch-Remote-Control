package com.javmarina.webrtc.signaling;

import com.javmarina.webrtc.JsonCodec;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;


public class SignalingPeer {

    // TODO: use wss://
    private static final String WEBSOCKET_ADDRESS = "ws://nintendo-switch-remote-control.appspot.com/signaling";

    private static final String COMMAND_REGISTER_OK = "register-ok";
    private static final String COMMAND_REGISTER_INVALID = "register-invalid";
    private static final String COMMAND_OFFER = "offer";
    private static final String COMMAND_ANSWER = "answer";
    private static final String COMMAND_NEW_ICE_CANDIDATE = "candidate";

    private static final String JSON_COMMAND = "command";
    private static final String JSON_SESSION_ID = "session-id";
    private static final String JSON_PAYLOAD = "payload";

    private final SessionId sessionId;
    private final HttpClient httpClient;
    private final WebSocketClient webSocketClient;
    private final String registerCommandId;
    private Session session;
    
    public SignalingPeer(final SessionId sessionId, final String registerCommandId) {
        this.sessionId = sessionId;
        this.httpClient = createHttpClient();
        this.webSocketClient = new WebSocketClient(this.httpClient);
        this.registerCommandId = registerCommandId;
    }

    private static HttpClient createHttpClient() {
        /* TODO: SSL
        final SslContextFactory sslContextFactory = new SslContextFactory();
        return new HttpClient(sslContextFactory);
         */
        return new HttpClient();
    }

    public void start() throws Exception {
        if (!httpClient.isRunning()) {
            try {
                httpClient.start();
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (!webSocketClient.isRunning()) {
            try {
                webSocketClient.start();
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public void connect(final Callback callback) throws Exception {
        final ClientUpgradeRequest request = new ClientUpgradeRequest();
        // Attempt connection
        final Future<Session> future = webSocketClient.connect(
                new ClientSocket(callback),
                new URI(WEBSOCKET_ADDRESS),
                request
        );
        // Wait for Connect
        session = future.get();
    }

    public void sendMessage(final String message) throws IOException {
        session.getRemote().sendString(message);
    }

    public void close() {
        session.close();
        session = null;
    }

    public void sendRegisterCommand() throws IOException {
        sendCommand(registerCommandId, null);
    }

    public void sendOffer(final RTCSessionDescription description) throws IOException {
        sendCommand(
                COMMAND_OFFER,
                JsonCodec.encode(description)
        );
    }

    public void sendAnswer(final RTCSessionDescription description) throws IOException {
        sendCommand(
                COMMAND_ANSWER,
                JsonCodec.encode(description)
        );
    }

    public void sendIceCandidate(final RTCIceCandidate candidate) throws IOException {
        sendCommand(
                COMMAND_NEW_ICE_CANDIDATE,
                JsonCodec.encode(candidate)
        );
    }

    public void sendCommand(final String commandId, final JSONObject payload) throws IOException {
        final JSONObject jo = new JSONObject();
        jo.put(JSON_COMMAND, commandId);
        jo.put(JSON_SESSION_ID, sessionId.id);
        jo.put(JSON_PAYLOAD, payload);
        sendMessage(jo.toString());
    }

    @SuppressWarnings("unused")
    @WebSocket(maxTextMessageSize = 64 * 1024)
    public static final class ClientSocket {

        private final Callback callback;
        private Session session;

        ClientSocket(final Callback callback) {
            this.callback = callback;
        }

        @OnWebSocketClose
        public void onClose(final int statusCode, final String reason) {
            this.session = null;
        }

        @OnWebSocketConnect
        public void onConnect(final Session session) {
            this.session = session;
        }

        @OnWebSocketMessage
        public void onMessage(final String msg) {
            final JSONObject jo = new JSONObject(msg);
            final String command = jo.getString(JSON_COMMAND);
            switch (command) {
                case COMMAND_REGISTER_INVALID:
                    session.close();
                    callback.onInvalidRegister();
                    break;
                case COMMAND_REGISTER_OK:
                    break;
                case COMMAND_OFFER:
                case COMMAND_ANSWER:
                    final RTCSessionDescription description =
                            JsonCodec.decodeSessionDescription(jo.getJSONObject(JSON_PAYLOAD));
                    if (COMMAND_OFFER.equals(command)) {
                        callback.onOfferReceived(description);
                    } else {
                        callback.onAnswerReceived(description);
                    }
                    break;
                case COMMAND_NEW_ICE_CANDIDATE:
                    final RTCIceCandidate candidate =
                            JsonCodec.decodeCandidate(jo.getJSONObject(JSON_PAYLOAD));
                    callback.onCandidateReceived(candidate);
                    break;
            }
        }
    }

    public interface Callback {
        void onOfferReceived(final RTCSessionDescription description);
        void onAnswerReceived(final RTCSessionDescription description);
        void onCandidateReceived(final RTCIceCandidate candidate);
        void onInvalidRegister();
    }
}
