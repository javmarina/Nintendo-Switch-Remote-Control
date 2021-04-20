package com.javmarina.webrtc;

import com.javmarina.util.Packet;
import com.javmarina.util.StoppableLoop;
import com.javmarina.webrtc.signaling.ClientSideSignaling;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioSource;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoSource;
import dev.onvoid.webrtc.media.video.VideoTrack;

import java.io.IOException;
import java.nio.ByteBuffer;


public class RtcClient extends RtcPeer<ClientSideSignaling> {

    private final PacketProvider packetProvider;
    private final RTCDataChannel dataChannel;
    private final ClientOut clientOutRunnable;
    private final Thread threadOut;
    private final Callback callback;

    public RtcClient(final ClientSideSignaling clientSideSignaling,
                     final PacketProvider packetProvider,
                     final Callback callback) {
        super(clientSideSignaling);
        this.packetProvider = packetProvider;
        this.callback = callback;

        final RTCDataChannelInit init = new RTCDataChannelInit();
        init.priority = RTCPriorityType.HIGH;
        dataChannel = peerConnection.createDataChannel("dataChannel", init);
        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(final long previousAmount) {
            }

            @Override
            public void onStateChange() {
            }

            @Override
            public void onMessage(final RTCDataChannelBuffer buffer) {
                final byte commandId = buffer.data.get(0);
                if (commandId == RtcPeer.COMMAND_PING) {
                    final int rtt = (int) (System.currentTimeMillis() - buffer.data.getLong(1));
                    callback.onRttReplyReceived(rtt);
                    clientOutRunnable.packetCounter = 0;
                }
            }
        });

        this.clientOutRunnable = new ClientOut();
        this.threadOut = new Thread(this.clientOutRunnable);

        createTransceivers();
    }

    private void createTransceivers() {
        final AudioSource audioSource = factory.createAudioSource(new AudioOptions());
        final AudioTrack audioTrack = factory.createAudioTrack("audioTrack", audioSource);
        final RTCRtpTransceiverInit audioTransceiverInit = new RTCRtpTransceiverInit();
        audioTransceiverInit.direction = RTCRtpTransceiverDirection.RECV_ONLY;
        audioTransceiverInit.streamIds.add("stream");
        final RTCRtpTransceiver audioTransceiver = peerConnection.addTransceiver(audioTrack, audioTransceiverInit);

        final VideoSource videoSource = new VideoDeviceSource();
        final VideoTrack videoTrack = factory.createVideoTrack("videoTrack", videoSource);
        final RTCRtpTransceiverInit videoTransceiverInit = new RTCRtpTransceiverInit();
        videoTransceiverInit.direction = RTCRtpTransceiverDirection.RECV_ONLY;
        videoTransceiverInit.streamIds.add("stream");
        final RTCRtpTransceiver videoTransceiver = peerConnection.addTransceiver(videoTrack, videoTransceiverInit);
    }

    @Override
    public void start() {
        try {
            baseSignaling.requestConnection();
            super.start();

            // Create offer
            final RTCOfferOptions offerOptions = new RTCOfferOptions();
            peerConnection.createOffer(offerOptions, new CreateSessionDescriptionObserver() {
                @Override
                public void onSuccess(final RTCSessionDescription description) {
                    peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                        @Override
                        public void onSuccess() {
                            try {
                                baseSignaling.sendOffer(description);
                            } catch (final IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(final String error) {
                            log(error);
                        }
                    });
                }

                @Override
                public void onFailure(final String error) {
                    log(error);
                }
            });
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onAnswerReceived(final RTCSessionDescription description) {
        peerConnection.setRemoteDescription(description, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(final String error) {
                log(error);
            }
        });
    }

    @Override
    public void onTrack(final RTCRtpTransceiver transceiver) {
        final MediaStreamTrack track = transceiver.getReceiver().getTrack();

        if (track.getKind().equals(MediaStreamTrack.VIDEO_TRACK_KIND)) {
            final VideoTrack videoTrack = (VideoTrack) track;
            videoTrack.addSink(callback);
        } else if (track.getKind().equals(MediaStreamTrack.AUDIO_TRACK_KIND)) {
            final AudioTrack audioTrack = (AudioTrack) track;
            audioTrack.addSink(callback);
        }
    }

    @Override
    protected void onConnected() {
        threadOut.start();
        callback.onSessionStarted();
    }

    @Override
    protected void onDisconnected() {
        clientOutRunnable.stop(callback::onSessionStopped);
    }

    private final class ClientOut extends StoppableLoop {

        private static final int PERIOD_MS = 5;
        private static final int RTT_PACKET_COUNTER = 50;

        private long millis = 0;
        /*
        When set to negative, counter is stopped. This happens when sending a PING. When the response is
        received, the counter is set back to 0. This is done in order to prevent computing RTT for wrong replies
        (when we send a new PING without having received the last reply).
         */
        private int packetCounter = 0;

        @Override
        public void loop() {
            try {
                if (System.currentTimeMillis() - millis > PERIOD_MS) {
                    final Packet packet = packetProvider.getPacket();
                    if (packet == null) {
                        peerConnection.close();
                        return;
                    } else {
                        final byte[] packetBuffer = packet.getBuffer();
                        final ByteBuffer byteBuffer = ByteBuffer.allocate(1 + packetBuffer.length);
                        byteBuffer.put(COMMAND_PACKET);
                        byteBuffer.put(packetBuffer);
                        final RTCDataChannelBuffer buffer = new RTCDataChannelBuffer(byteBuffer, true);
                        dataChannel.send(buffer);
                        millis = System.currentTimeMillis();
                        if (packetCounter >= 0) {
                            packetCounter++;
                        }
                    }
                }

                if (packetCounter > RTT_PACKET_COUNTER) {
                    packetCounter = -1;
                    final ByteBuffer byteBuffer = ByteBuffer.allocate(1 + Long.BYTES);
                    byteBuffer.put(0, COMMAND_PING);
                    byteBuffer.putLong(1, System.currentTimeMillis());
                    final RTCDataChannelBuffer buffer = new RTCDataChannelBuffer(byteBuffer, true);
                    dataChannel.send(buffer);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface PacketProvider {

        /**
         * Called when a new {@link Packet} is to be sent to the sever.
         * @return the packet to send, or {@code null} if no more packets can be sent
         * and the connection should be closed.
         */
        Packet getPacket();
    }

    public interface Callback extends AudioVideoSink {
        /**
         * New RTT value computed.
         * @param milliseconds new RTT in milliseconds.
         */
        void onRttReplyReceived(final int milliseconds);

        /**
         * Connection with server has been established.
         */
        void onSessionStarted();

        /**
         * Connection with server successfully closed.
         */
        void onSessionStopped();
    }
}
