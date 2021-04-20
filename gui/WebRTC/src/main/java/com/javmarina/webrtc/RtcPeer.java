package com.javmarina.webrtc;

import com.javmarina.util.StoppableLoop;
import com.javmarina.util.network.protocol.Command;
import com.javmarina.webrtc.signaling.BaseSignaling;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public abstract class RtcPeer<T extends BaseSignaling> {

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

    protected final T baseSignaling;
    private final CommandProcessing commandProcessing = new CommandProcessing();
    private final Thread commandThread;
    protected final PeerConnectionFactory factory;
    protected RTCPeerConnection peerConnection;

    public RtcPeer(final T baseSignaling) {
        this(baseSignaling, null);
    }

    public RtcPeer(final T baseSignaling, final AudioDeviceModule audioDeviceModule) {
        this.baseSignaling = baseSignaling;
        commandThread = new Thread(commandProcessing);

        factory = audioDeviceModule != null ?
                new PeerConnectionFactory(audioDeviceModule) : new PeerConnectionFactory();
        peerConnection = factory.createPeerConnection(defaultConfiguration, new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(final RTCIceCandidate candidate) {
                try {
                    baseSignaling.sendIceCandidate(candidate);
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
                System.out.println("[onConnectionChange] " + state);
                switch (state) {
                    case CONNECTED:
                        onConnected();
                        break;
                    case DISCONNECTED:
                        onDisconnected();
                        break;
                }
            }

            @Override
            public void onIceConnectionChange(final RTCIceConnectionState state) {
                System.out.println("[onIceConnectionChange] " + state);
            }

            @Override
            public void onTrack(final RTCRtpTransceiver transceiver) {
                RtcPeer.this.onTrack(transceiver);
            }
        });
    }

    public void start() {
        commandThread.start();
    }

    public void stop() {
        commandProcessing.stop();
    }

    public static void log(final String error) {
        System.out.println(error);
    }

    public static void log(final Exception e) {
        e.printStackTrace();
    }

    protected void onOfferReceived(final RTCSessionDescription description) {}
    protected void onAnswerReceived(final RTCSessionDescription description) {}
    protected void onDataChannel(final RTCDataChannel dataChannel) {}
    protected void onTrack(final RTCRtpTransceiver transceiver) {}

    protected void onConnected() {
        try {
            baseSignaling.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void onDisconnected();

    protected void onCandidateReceived(final RTCIceCandidate candidate) {
        peerConnection.addIceCandidate(candidate);
    }

    private final class CommandProcessing extends StoppableLoop {

        @Override
        public void loop() {
            try {
                final Command command = baseSignaling.receiveCommand();
                if (command == null) {
                    return;
                }
                switch (command.getId()) {
                    case BaseSignaling.OFFER_COMMAND:
                        final RTCSessionDescription description =
                                (RTCSessionDescription) JsonCodec.decode(new String(command.getPayload(), StandardCharsets.UTF_8));
                        onOfferReceived(description);
                        break;
                    case BaseSignaling.ANSWER_COMMAND:
                        final RTCSessionDescription answerDescription =
                                (RTCSessionDescription) JsonCodec.decode(new String(command.getPayload(), StandardCharsets.UTF_8));
                        onAnswerReceived(answerDescription);
                        break;
                    case BaseSignaling.NEW_ICE_CANDIDATE_COMMAND:
                        final RTCIceCandidate candidate =
                                (RTCIceCandidate) JsonCodec.decode(new String(command.getPayload(), StandardCharsets.UTF_8));
                        onCandidateReceived(candidate);
                        break;
                    default:
                        throw new RuntimeException("Unknown ID: " + command.getId());
                }
            } catch (final IOException e) {
                // Not a fatal error
                e.printStackTrace();
            }
        }
    }
}
