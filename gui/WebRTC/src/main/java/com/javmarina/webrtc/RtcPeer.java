package com.javmarina.webrtc;

import com.javmarina.webrtc.signaling.SignalingPeer;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public abstract class RtcPeer {

    // Data channel
    protected static final byte COMMAND_PACKET = 0x33;
    protected static final byte COMMAND_PING = 0x44;

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
    protected RTCPeerConnection peerConnection;

    public RtcPeer(final SignalingPeer signalingPeer) {
        this(signalingPeer, null);
    }

    public RtcPeer(final SignalingPeer signalingPeer, final AudioDeviceModule audioDeviceModule) {
        this.signalingPeer = signalingPeer;

        factory = audioDeviceModule != null ?
                new PeerConnectionFactory(audioDeviceModule) : new PeerConnectionFactory();
        peerConnection = factory.createPeerConnection(defaultConfiguration, new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(final RTCIceCandidate candidate) {
                try {
                    signalingPeer.sendIceCandidate(candidate);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDataChannel(final RTCDataChannel dataChannel) {
                RtcPeer.this.onDataChannel(dataChannel);
            }

            @Override
            public void onConnectionChange(final RTCPeerConnectionState state) {
                if (state == RTCPeerConnectionState.CONNECTED) {
                    onConnected();
                }
                if (state == RTCPeerConnectionState.CLOSED || state == RTCPeerConnectionState.DISCONNECTED) {
                    onDisconnected();
                }
            }

            @Override
            public void onTrack(final RTCRtpTransceiver transceiver) {
                RtcPeer.this.onTrack(transceiver);
            }
        });
    }

    public void start() throws Exception {
        signalingPeer.start();
        signalingPeer.connect(new SignalingPeer.Callback() {
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
        });
    }

    public void stop() {
        peerConnection.close();
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
    protected abstract void onInvalidSessionId();
}
