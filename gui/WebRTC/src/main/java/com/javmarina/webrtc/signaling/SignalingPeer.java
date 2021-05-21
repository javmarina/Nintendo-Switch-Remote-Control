package com.javmarina.webrtc.signaling;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.javmarina.webrtc.JsonCodec;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class SignalingPeer {

    static {
        final FirebaseOptions options;
        try {
            final InputStream stream = SignalingPeer.class.getClassLoader().getResourceAsStream("serviceAccountKey.json");
            if (stream != null) {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .setDatabaseUrl("https://nintendo-switch-signaling-default-rtdb.europe-west1.firebasedatabase.app")
                        .build();
                FirebaseApp.initializeApp(options);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static final String COMMAND_OFFER = "offer";
    private static final String COMMAND_ANSWER = "answer";
    private static final String COMMAND_NEW_ICE_CANDIDATE = "candidate";

    private final SessionId sessionId;
    private final Role role;
    private EventListener eventListener;

    public enum Role {
        CLIENT, SERVER;

        public String getRegisterCommand() {
            if (this == CLIENT) {
                return "register-client";
            } else {
                return "register-server";
            }
        }

        public String getOutChild() {
            if (this == CLIENT) {
                return "toServer";
            } else {
                return "toClient";
            }
        }

        public String getInChild() {
            if (this == CLIENT) {
                return "toClient";
            } else {
                return "toServer";
            }
        }
    }
    
    public SignalingPeer(final SessionId sessionId, final Role role) {
        this.sessionId = sessionId;
        this.role = role;
    }

    public void start(final Callback callback) {
        final DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("sessions");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                final boolean exists = dataSnapshot.hasChild(sessionId.toString());
                switch (role) {
                    case SERVER:
                        if (exists) {
                            callback.onInvalidRegister();
                        } else {
                            final Map<String, Object> data = new HashMap<>(2);
                            data.put(role.getRegisterCommand(), "ok");
                            data.put(role.getOutChild(), 0);
                            ref.child(sessionId.toString()).updateChildren(data, (error, ref1) -> callback.onValidRegister());
                            eventListener = new EventListener(callback);
                            ref.child(sessionId.toString()).child(role.getInChild()).addChildEventListener(eventListener);
                        }
                        break;
                    case CLIENT:
                        if (exists) {
                            final Map<String, Object> data = new HashMap<>(2);
                            data.put(role.getRegisterCommand(), "ok");
                            data.put(role.getOutChild(), 0);
                            ref.child(sessionId.toString()).updateChildren(data, (error, ref12) -> callback.onValidRegister());
                            eventListener = new EventListener(callback);
                            ref.child(sessionId.toString()).child(role.getInChild()).addChildEventListener(eventListener);
                        } else {
                            callback.onInvalidRegister();
                        }
                        break;
                }
            }

            @Override
            public void onCancelled(final DatabaseError error) {
                callback.onInvalidRegister();
            }
        });
    }

    private static final class EventListener implements ChildEventListener {

        private final Callback callback;

        private EventListener(final Callback callback) {
            this.callback = callback;
        }

        private void processInput(final DataSnapshot dataSnapshot) {
            final String key = dataSnapshot.getKey();
            final JSONObject jo = new JSONObject(dataSnapshot.getValue().toString());
            switch (key) {
                case COMMAND_OFFER:
                    final RTCSessionDescription offer = JsonCodec.decodeSessionDescription(jo);
                    callback.onOfferReceived(offer);
                    break;
                case COMMAND_ANSWER:
                    final RTCSessionDescription answer = JsonCodec.decodeSessionDescription(jo);
                    callback.onAnswerReceived(answer);
                    break;
                case COMMAND_NEW_ICE_CANDIDATE:
                    final RTCIceCandidate candidate = JsonCodec.decodeCandidate(jo);
                    callback.onCandidateReceived(candidate);
                    break;
            }
        }

        @Override
        public void onChildAdded(final DataSnapshot snapshot, final String previousChildName) {
            processInput(snapshot);
        }

        @Override
        public void onChildChanged(final DataSnapshot snapshot, final String previousChildName) {
            processInput(snapshot);
        }

        @Override
        public void onChildRemoved(final DataSnapshot snapshot) {
        }

        @Override
        public void onChildMoved(final DataSnapshot snapshot, final String previousChildName) {
        }

        @Override
        public void onCancelled(final DatabaseError error) {
        }
    }

    public void close() {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("sessions")
                .child(sessionId.toString());
        databaseReference.child(role.getInChild())
                .removeEventListener(eventListener);
        databaseReference.removeValueAsync();
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
        final Map<String, Object> map = new HashMap<>(1);
        map.put(commandId, payload.toString());
        FirebaseDatabase.getInstance()
                .getReference("sessions")
                .child(sessionId.toString())
                .child(role.getOutChild())
                .updateChildrenAsync(map);
    }

    public interface Callback {
        void onOfferReceived(final RTCSessionDescription description);
        void onAnswerReceived(final RTCSessionDescription description);
        void onCandidateReceived(final RTCIceCandidate candidate);
        void onInvalidRegister();
        void onValidRegister();
    }
}
