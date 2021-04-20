package com.javmarina.webrtc;

import com.google.gson.*;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;

import java.lang.reflect.Type;
import java.util.Locale;

public final class JsonCodec {

    private static class SdpTypeSerializer implements JsonSerializer<RTCSdpType> {
        @Override
        public JsonElement serialize(final RTCSdpType src, final Type typeOfSrc, final JsonSerializationContext context) {
            return new JsonPrimitive(src.toString().toLowerCase(Locale.ROOT));
        }
    }

    private static class SdpTypeDeserializer implements JsonDeserializer<RTCSdpType> {
        @Override
        public RTCSdpType deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final String type = json.getAsJsonPrimitive().getAsString();
            final RTCSdpType rtcType;
            if (type.equals("offer")) {
                rtcType = RTCSdpType.OFFER;
            } else if (type.equals("answer")) {
                rtcType = RTCSdpType.ANSWER;
            } else {
                throw new JsonParseException("Invalid RTCSdpType");
            }
            return rtcType;
        }
    }

    public static String encode(final RTCSessionDescription sessionDescription) {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(RTCSdpType.class, new SdpTypeSerializer());
        builder.registerTypeAdapter(RTCSdpType.class, new SdpTypeDeserializer());
        return builder.create().toJson(sessionDescription);
    }

    public static String encode(final RTCIceCandidate candidate) {
        return new Gson().toJson(candidate);
    }

    public static Object decode(final String json) {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(RTCSdpType.class, new SdpTypeSerializer());
        builder.registerTypeAdapter(RTCSdpType.class, new SdpTypeDeserializer());
        final Gson gson = builder.create();
        if (json.contains("sdpMid")) {
            return gson.fromJson(json, RTCIceCandidate.class);
        } else {
            return gson.fromJson(json, RTCSessionDescription.class);
        }
    }
}
