package com.javmarina.webrtc.signaling;

import com.javmarina.webrtc.JsonCodec;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.Polling;
import org.json.JSONObject;

import java.net.URI;


public class SignalingPeer {

    // Put your signaling server URI here
    private static final String SIGNALING_SERVER = "";

    private static final String COMMAND_REGISTER_OK = "register-ok";
    private static final String COMMAND_REGISTER_INVALID = "register-invalid";
    private static final String COMMAND_OFFER = "offer";
    private static final String COMMAND_ANSWER = "answer";
    private static final String COMMAND_NEW_ICE_CANDIDATE = "candidate";

    private final SessionId sessionId;
    private final Role role;
    private Socket socket;

    public enum Role {
        CLIENT, SERVER;

        public String getRegisterCommand() {
            if (this == CLIENT) {
                return "register-client";
            } else {
                return "register-server";
            }
        }
    }
    
    public SignalingPeer(final SessionId sessionId, final Role role) {
        this.sessionId = sessionId;
        this.role = role;
    }

    public void start(final Callback callback) {
        final IO.Options options = IO.Options.builder()
                .setTransports(new String[]{Polling.NAME})
                .build();
        final URI uri = URI.create(SIGNALING_SERVER);
        socket = IO.socket(uri, options);
        socket.on(Socket.EVENT_CONNECT, args ->
                        socket.emit(role.getRegisterCommand(), sessionId.toString()
        )).on(Socket.EVENT_CONNECT_ERROR, arg0 ->
                System.out.println("EVENT_CONNECT_ERROR " +  arg0[0].toString()
        )).on(COMMAND_OFFER, args -> {
            final JSONObject jo = new JSONObject((String) args[0]);
            final RTCSessionDescription offer = JsonCodec.decodeSessionDescription(jo);
            callback.onOfferReceived(offer);
        }).on(COMMAND_ANSWER, args -> {
            final JSONObject jo = new JSONObject((String) args[0]);
            final RTCSessionDescription answer = JsonCodec.decodeSessionDescription(jo);
            callback.onAnswerReceived(answer);
        }).on(COMMAND_NEW_ICE_CANDIDATE, args -> {
            final JSONObject jo = new JSONObject((String) args[0]);
            final RTCIceCandidate candidate = JsonCodec.decodeCandidate(jo);
            callback.onCandidateReceived(candidate);
        }).on(COMMAND_REGISTER_INVALID, args -> {
            callback.onInvalidRegister();
            socket.close();
        }).on(COMMAND_REGISTER_OK, args -> callback.onValidRegister());
        socket.connect();
    }

    public void close() {
        socket.close();
    }

    public void sendOffer(final RTCSessionDescription description) {
        sendCommand(
                COMMAND_OFFER,
                JsonCodec.encode(description)
        );
    }

    public void sendAnswer(final RTCSessionDescription description) {
        sendCommand(
                COMMAND_ANSWER,
                JsonCodec.encode(description)
        );
    }

    public void sendIceCandidate(final RTCIceCandidate candidate) {
        sendCommand(
                COMMAND_NEW_ICE_CANDIDATE,
                JsonCodec.encode(candidate)
        );
    }

    private void sendCommand(final String commandId, final JSONObject payload) {
        socket.emit(commandId, payload.toString());
    }

    public interface Callback {
        void onOfferReceived(final RTCSessionDescription description);
        void onAnswerReceived(final RTCSessionDescription description);
        void onCandidateReceived(final RTCIceCandidate candidate);
        void onInvalidRegister();
        void onValidRegister();
    }
}
