package com.javmarina.webrtc;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;


public final class JsonCodec {

    public static JSONObject encode(final RTCSessionDescription sessionDescription) {
        final JSONObject jo = new JSONObject();
        jo.put("sdpType", sessionDescription.sdpType.toString().toLowerCase(Locale.ROOT));
        jo.put("sdp", sessionDescription.sdp);
        return jo;
    }

    public static RTCSessionDescription decodeSessionDescription(final JSONObject payload) {
        final String type = payload.getString("sdpType");
        final RTCSdpType rtcType;
        if (type.equals("offer")) {
            rtcType = RTCSdpType.OFFER;
        } else if (type.equals("answer")) {
            rtcType = RTCSdpType.ANSWER;
        } else {
            throw new JSONException("Invalid RTCSdpType");
        }
        return new RTCSessionDescription(
                rtcType,
                payload.getString("sdp")
        );
    }

    public static JSONObject encode(final RTCIceCandidate candidate) {
        final JSONObject jo = new JSONObject();
        jo.put("sdpMid", candidate.sdpMid);
        jo.put("sdpMLineIndex", candidate.sdpMLineIndex);
        jo.put("sdp", candidate.sdp);
        jo.put("serverUrl", candidate.serverUrl);
        return jo;
    }

    public static RTCIceCandidate decodeCandidate(final JSONObject payload) {
        return new RTCIceCandidate(
                payload.getString("sdpMid"),
                payload.getInt("sdpMLineIndex"),
                payload.getString("sdp"),
                payload.has("serverUrl") ? payload.getString("serverUrl") : null
        );
    }
}
