package com.javmarina.webrtc;

import com.javmarina.util.Packet;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoFrame;

import java.util.List;


// https://webrtc.org/getting-started/peer-connections-advanced
// https://github.com/devopvoid/webrtc-java/blob/master/webrtc-demo/webrtc-demo-api/src/main/java/dev/onvoid/webrtc/demo/net/PeerConnectionClient.java
// https://w3c.github.io/webrtc-pc/
// https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Connectivity
public class WebRtcPlayground {

    public static void main(final String... args) {
        WebRtcLoader.loadLibrary();

        testDevices();

        final AudioDeviceModule deviceModule = new AudioDeviceModule();
        final AudioDevice audioDevice = MediaDevices.getAudioCaptureDevices().get(0);
        deviceModule.setRecordingDevice(audioDevice);
        final List<VideoDevice> videoDevices = MediaDevices.getVideoCaptureDevices();
        final VideoDevice videoDevice = videoDevices.get(0);
        final VideoDeviceSource videoSource = new VideoDeviceSource();
        videoSource.setVideoCaptureDevice(videoDevice);
        final VideoCaptureCapability capability = VideoCapabilitySelection.selectCapability(
                MediaDevices.getVideoCaptureCapabilities(videoDevice),
                VideoCapabilitySelection.Policy.BEST_RESOLUTION
        );
        videoSource.setVideoCaptureCapability(capability);
        final RtcServer server = new RtcServer(SessionId.fromString("0000"), deviceModule, videoSource,
                new RtcServer.Callback() {
                    @Override
                    public void onPacketReceived(final Packet packet) {
                    }

                    @Override
                    public void onSessionStarted() {
                    }

                    @Override
                    public void onSessionStopped() {
                    }

                    @Override
                    public void onError(final Exception e) {
                    }

                    @Override
                    public void onInvalidSessionId() {
                    }
                });

        final RtcClient client = new RtcClient(
                SessionId.fromString("0000"),
                Packet.Companion::getEMPTY_PACKET,
                new RtcClient.Callback() {
                    @Override
                    public void onRttReplyReceived(final int milliseconds) {
                    }

                    @Override
                    public void onSessionStarted() {

                    }

                    @Override
                    public void onSessionStopped() {

                    }

                    @Override
                    public void onInvalidSessionId() {

                    }

                    @Override
                    public void onVideoFrame(final VideoFrame frame) {
                    }
                }
        );

        new Thread(() -> {
            try {
                server.start();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                client.start();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void testDevices() {
        final List<VideoDevice> videoDevices = MediaDevices.getVideoCaptureDevices();
        final List<AudioDevice> audioDevices = MediaDevices.getAudioCaptureDevices();

        System.out.println("Video devices:");
        for (final VideoDevice device : videoDevices) {
            System.out.println(device);
        }

        System.out.println("Audio devices:");
        for (final AudioDevice device : audioDevices) {
            System.out.println(device);
        }

        final VideoDevice device = videoDevices.get(0);

        final VideoDeviceSource videoSource = new VideoDeviceSource();
        videoSource.setVideoCaptureDevice(device);
        final VideoCaptureCapability capability = VideoCapabilitySelection.selectCapability(
                MediaDevices.getVideoCaptureCapabilities(device),
                VideoCapabilitySelection.Policy.BEST_RESOLUTION
        );
        videoSource.setVideoCaptureCapability(capability);
        videoSource.start();

        try {
            Thread.sleep(5);
            videoSource.stop();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}
