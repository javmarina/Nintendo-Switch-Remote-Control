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
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioSource;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;

import java.io.IOException;
import java.util.List;


public class RtcServer extends RtcPeer {

    private final VideoDeviceSource videoSource;
    private final AudioDeviceModule audioDeviceModule;
    private final Callback callback;

    public RtcServer(final SessionId sessionId,
                     final AudioDeviceModule audioDeviceModule,
                     final VideoDeviceSource videoSource,
                     final Callback callback) {
        super(new SignalingPeer(sessionId, "register-server"), audioDeviceModule);
        this.audioDeviceModule = audioDeviceModule;
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
        final AudioTrack audioTrack = factory.createAudioTrack("audioTrack", audioSource);
        final RTCRtpSender audioSender = peerConnection.addTrack(audioTrack, List.of("stream"));

        // Add video
        final VideoTrack videoTrack = factory.createVideoTrack("videoTrack", videoSource);
        final RTCRtpSender videoSender = peerConnection.addTrack(videoTrack, List.of("stream"));

        // TODO: we can block receiving media, even though client won't offer it
        /* for (final RTCRtpTransceiver transceiver : peerConnection.getTransceivers()) {
            transceiver.setDirection(RTCRtpTransceiverDirection.SEND_ONLY);
        }*/
    }

    @Override
    public void start() throws Exception {
        super.start();
        signalingPeer.sendRegisterCommand();
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
                                try {
                                    signalingPeer.sendAnswer(description);
                                } catch (final IOException e) {
                                    callback.onError(e);
                                }
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
                }
            }
        });
    }

    @Override
    protected void onConnected() {
        super.onConnected();
        videoSource.start();
        audioDeviceModule.initRecording();
        callback.onSessionStarted();
    }

    @Override
    protected void onDisconnected() {
        videoSource.stop();
        videoSource.dispose();
        audioDeviceModule.dispose();
        callback.onSessionStopped();
    }

    @Override
    protected void onInvalidSessionId() {
        callback.onInvalidSessionId();
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
