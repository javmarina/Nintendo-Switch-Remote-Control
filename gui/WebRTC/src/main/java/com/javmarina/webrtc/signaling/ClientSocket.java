package com.javmarina.webrtc.signaling;

import com.javmarina.webrtc.JsonCodec;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;


@SuppressWarnings("unused")
@WebSocket(maxTextMessageSize = 64 * 1024)
public final class ClientSocket {

    private final SignalingPeer.Callback callback;
    private Session session;

    ClientSocket(final SignalingPeer.Callback callback) {
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
        final String command = jo.getString("command");
        switch (command) {
            case "register-invalid":
                session.close();
                callback.onInvalidRegister();
                break;
            case "register-ok":
                break;
            case "offer":
            case "answer":
                final RTCSessionDescription description =
                        JsonCodec.decodeSessionDescription(jo.getJSONObject("payload"));
                if ("offer".equals(command)) {
                    callback.onOfferReceived(description);
                } else {
                    callback.onAnswerReceived(description);
                }
                break;
            case "candidate":
                final RTCIceCandidate candidate =
                        JsonCodec.decodeCandidate(jo.getJSONObject("payload"));
                callback.onCandidateReceived(candidate);
                break;
        }
    }
}
