package com.javmarina.webrtc.signaling;

import com.javmarina.webrtc.JsonCodec;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.api.Session;
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

    public static final String OFFER_COMMAND = "offer";
    public static final String ANSWER_COMMAND = "answer";
    public static final String NEW_ICE_CANDIDATE_COMMAND = "candidate";

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

    public void connect(final SignalingPeer.Callback callback) throws Exception {
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
                OFFER_COMMAND,
                JsonCodec.encode(description)
        );
    }

    public void sendAnswer(final RTCSessionDescription description) throws IOException {
        sendCommand(
                ANSWER_COMMAND,
                JsonCodec.encode(description)
        );
    }

    public void sendIceCandidate(final RTCIceCandidate candidate) throws IOException {
        sendCommand(
                NEW_ICE_CANDIDATE_COMMAND,
                JsonCodec.encode(candidate)
        );
    }

    public void sendCommand(final String commandId, final JSONObject payload) throws IOException {
        final JSONObject jo = new JSONObject();
        jo.put("command", commandId);
        jo.put("session-id", sessionId.id);
        jo.put("payload", payload);
        sendMessage(jo.toString());
    }

    public interface Callback {
        void onOfferReceived(final RTCSessionDescription description);
        void onAnswerReceived(final RTCSessionDescription description);
        void onCandidateReceived(final RTCIceCandidate candidate);
        void onInvalidRegister();
    }
}
