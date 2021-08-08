package com.javmarina.webrtc;

import com.javmarina.util.Packet;
import com.javmarina.webrtc.signaling.SignalingPeer;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCRtpSender;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCRtpTransceiverDirection;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioSource;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;

import java.util.List;


public class RtcServer extends RtcPeer {

    private final VideoDeviceSource videoSource;
    private final Callback callback;

    public RtcServer(final SessionId sessionId,
                     final AudioDeviceModule audioDeviceModule,
                     final VideoDeviceSource videoSource,
                     final Callback callback) {
        super(new SignalingPeer(sessionId, SignalingPeer.Role.SERVER), audioDeviceModule);
        this.videoSource = videoSource;
        this.callback = callback;
        createTransceivers();
    }

    private void createTransceivers() {
        final AudioOptions audioOptions = new AudioOptions();
        audioOptions.echoCancellation = false;
        audioOptions.autoGainControl = false;
        audioOptions.noiseSuppression = false;
        final AudioSource audioSource = factory.createAudioSource(audioOptions);
        final AudioTrack audioTrack = factory.createAudioTrack(AUDIO_TRACK_NAME, audioSource);
        final RTCRtpSender audioSender = peerConnection.addTrack(audioTrack, List.of(STREAM_ID));

        // Add video
        final VideoTrack videoTrack = factory.createVideoTrack(VIDEO_TRACK_NAME, videoSource);
        final RTCRtpSender videoSender = peerConnection.addTrack(videoTrack, List.of(STREAM_ID));

        /* Doesn't currently work
        final List<RTCRtpCodecCapability> videoCodecs = factory.getRtpSenderCapabilities(MediaType.VIDEO)
                .getCodecs()
                .stream()
                .sorted((o1, o2) -> {
                    final boolean is1 = o1.getName().contains("VP9");
                    final boolean is2 = o2.getName().contains("VP9");
                    if (is1 && is2) {
                        return -Integer.compare(
                                Integer.parseInt(o1.getSDPFmtp().get("profile-id")),
                                Integer.parseInt(o2.getSDPFmtp().get("profile-id"))
                        );
                    } else if (is1) {
                        return -1;
                    } else if (is2) {
                        return 1;
                    } else {
                        return 0;
                    }
                })
                .collect(Collectors.toList());

        for (final RTCRtpTransceiver transceiver : peerConnection.getTransceivers()) {
            if (transceiver.getSender().getTrack().getKind().equals(MediaStreamTrack.VIDEO_TRACK_KIND)) {
                transceiver.setCodecPreferences(videoCodecs);
            }
        }*/

        // Block incoming media streams
        for (final RTCRtpTransceiver transceiver : peerConnection.getTransceivers()) {
            transceiver.setDirection(RTCRtpTransceiverDirection.SEND_ONLY);
        }
    }

    @Override
    protected void onOfferReceived(final RTCSessionDescription description) {
        peerConnection.setRemoteDescription(description, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                final RTCAnswerOptions answerOptions = new RTCAnswerOptions();
                peerConnection.createAnswer(answerOptions, new CreateSessionDescriptionObserver() {
                    @Override
                    public void onSuccess(final RTCSessionDescription description) {
                        peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                            @Override
                            public void onSuccess() {
                                signalingPeer.sendAnswer(description);
                            }

                            @Override
                            public void onFailure(final String error) {
                                callback.onError(new Exception(error));
                            }
                        });
                    }

                    @Override
                    public void onFailure(final String error) {
                        callback.onError(new Exception(error));
                    }
                });
            }

            @Override
            public void onFailure(final String error) {
                callback.onError(new Exception(error));
            }
        });
    }

    @Override
    protected void onDataChannel(final RTCDataChannel dataChannel) {
        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(final long previousAmount) {
            }

            @Override
            public void onStateChange() {
            }

            @Override
            public void onMessage(final RTCDataChannelBuffer buffer) {
                final byte commandId = buffer.data.get();
                switch (commandId) {
                    case COMMAND_PACKET:
                        final byte[] packetBuffer = new byte[8];
                        buffer.data.get(packetBuffer);
                        callback.onPacketReceived(new Packet(packetBuffer));
                        break;
                    case COMMAND_PING:
                        // Send same message
                        try {
                            dataChannel.send(buffer);
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        });
    }

    @Override
    protected void onConnected() {
        super.onConnected();
        videoSource.start();
        callback.onSessionStarted();
    }

    @Override
    protected void onDisconnected() {
        videoSource.stop();
        callback.onSessionStopped();
    }

    @Override
    protected void onInvalidSessionId() {
        callback.onInvalidSessionId();
    }

    @Override
    protected void onValidRegister() {
    }

    public interface Callback {
        /**
         * New packet received from client.
         * @param packet the received packet.
         */
        void onPacketReceived(final Packet packet);

        /**
         * Client requested a connection and it has been established successfully.
         */
        void onSessionStarted();

        /**
         * Session has been stopped correctly.
         */
        void onSessionStopped();

        /**
         * An error occurred.
         * @param e the error.
         */
        void onError(final Exception e);

        /**
         * Called if the selected session ID is not valid (already in use).
         */
        void onInvalidSessionId();
    }
}
