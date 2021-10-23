package com.javmarina.webrtc;

import com.javmarina.webrtc.signaling.SignalingPeer;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public abstract class RtcPeer {

    // Data channel
    protected static final byte COMMAND_PACKET = 0x33;
    protected static final byte COMMAND_PING = 0x44;

    private enum State {
        READY,
        CONNECTED,
        DISCONNECTED,
        CLOSED
    }

    private State peerState = State.READY;

    protected static final String AUDIO_TRACK_NAME = "audioTrack";
    protected static final String VIDEO_TRACK_NAME = "videoTrack";
    protected static final String DATA_CHANNEL_NAME = "dataChannel";
    protected static final String STREAM_ID = "stream";

    private static final RTCConfiguration defaultConfiguration;
    static {
        final String[] urls = {
                "stun:stun.l.google.com:19302",
                "stun:stun1.l.google.com:19302",
                "stun:stun2.l.google.com:19302",
                "stun:stun3.l.google.com:19302",
                "stun:stun4.l.google.com:19302"
        };
        final List<RTCIceServer> iceServers = Arrays.stream(urls).map(url -> {
            final RTCIceServer server = new RTCIceServer();
            server.urls = Collections.singletonList(url);
            return server;
        }).collect(Collectors.toList());

        defaultConfiguration = new RTCConfiguration();
        defaultConfiguration.iceServers = iceServers;
    }

    protected final SignalingPeer signalingPeer;
    protected final PeerConnectionFactory factory;
    protected final AudioDeviceModule audioDeviceModule;
    protected RTCPeerConnection peerConnection;

    public RtcPeer(final SignalingPeer signalingPeer, final AudioDeviceModule audioDeviceModule) {
        this.signalingPeer = signalingPeer;
        Objects.requireNonNull(audioDeviceModule);
        this.audioDeviceModule = audioDeviceModule;

        factory = new PeerConnectionFactory(this.audioDeviceModule);
        peerConnection = factory.createPeerConnection(defaultConfiguration, new PeerConnectionObserver() {
            @Override
            public void onIceConnectionChange(final RTCIceConnectionState state) {
                if (state == RTCIceConnectionState.DISCONNECTED && peerState == State.CONNECTED) {
                    peerState = State.DISCONNECTED;
                    onDisconnected();
                }
                if (state == RTCIceConnectionState.CLOSED &&
                        (peerState == State.DISCONNECTED || peerState == State.CONNECTED)) {
                    // peerState can be CONNECTED if this peer explicitly closes the connection, for example
                    // by calling RtcClient.stop()
                    peerState = State.CLOSED;
                    onClosed();
                }
            }

            @Override
            public void onIceCandidate(final RTCIceCandidate candidate) {
                signalingPeer.sendIceCandidate(candidate);
            }

            @Override
            public void onDataChannel(final RTCDataChannel dataChannel) {
                RtcPeer.this.onDataChannel(dataChannel);
            }

            @Override
            public void onConnectionChange(final RTCPeerConnectionState state) {
                if (state == RTCPeerConnectionState.CONNECTED && peerState == State.READY) {
                    peerState = State.CONNECTED;
                    onConnected();
                }
            }

            @Override
            public void onTrack(final RTCRtpTransceiver transceiver) {
                RtcPeer.this.onTrack(transceiver);
            }
        });
    }

    public void start() {
        signalingPeer.start(new SignalingPeer.Callback() {
            @Override
            public void onOfferReceived(final RTCSessionDescription description) {
                RtcPeer.this.onOfferReceived(description);
            }

            @Override
            public void onAnswerReceived(final RTCSessionDescription description) {
                RtcPeer.this.onAnswerReceived(description);
            }

            @Override
            public void onCandidateReceived(final RTCIceCandidate candidate) {
                peerConnection.addIceCandidate(candidate);
            }

            @Override
            public void onInvalidRegister() {
                RtcPeer.this.onInvalidSessionId();
            }

            @Override
            public void onValidRegister() {
                RtcPeer.this.onValidRegister();
            }
        });
    }

    public static void log(final String error) {
        log(new Exception(error));
    }

    public static void log(final Exception e) {
        e.printStackTrace();
    }

    protected void onOfferReceived(final RTCSessionDescription description) {}
    protected void onAnswerReceived(final RTCSessionDescription description) {}
    protected void onDataChannel(final RTCDataChannel dataChannel) {}
    protected void onTrack(final RTCRtpTransceiver transceiver) {}

    protected void onConnected() {
        signalingPeer.close();
    }

    protected abstract void onDisconnected();
    protected abstract void onClosed();
    protected abstract void onInvalidSessionId();
    protected abstract void onValidRegister();
}
