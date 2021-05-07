package com.javmarina.webrtc;

import com.javmarina.util.Packet;
import com.javmarina.webrtc.signaling.BaseSignaling;
import com.javmarina.webrtc.signaling.ClientSideSignaling;
import com.javmarina.webrtc.signaling.ServerSideSignaling;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoFrame;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;


// https://webrtc.org/getting-started/peer-connections-advanced
// https://github.com/devopvoid/webrtc-java/blob/master/webrtc-demo/webrtc-demo-api/src/main/java/dev/onvoid/webrtc/demo/net/PeerConnectionClient.java
// https://w3c.github.io/webrtc-pc/
// https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Connectivity
public class WebRtcPlayground {

    public static void main(final String... args) {
        WebRtcLoader.loadLibrary();

        testDevices();

        ServerSideSignaling serverSideSignaling = null;
        try {
            serverSideSignaling = new ServerSideSignaling(BaseSignaling.DEFAULT_PORT);
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }
        final AudioDeviceModule deviceModule = new AudioDeviceModule();
        final AudioDevice audioDevice = MediaDevices.getAudioCaptureDevices().get(0);
        deviceModule.setRecordingDevice(audioDevice);
        final List<VideoDevice> videoDevices = MediaDevices.getVideoCaptureDevices();
        final VideoDevice videoDevice = videoDevices.get(0);
        final RtcServer server = new RtcServer(serverSideSignaling, deviceModule, videoDevice,
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
                });

        ClientSideSignaling clientSideSignaling = null;
        try {
            clientSideSignaling = new ClientSideSignaling("127.0.0.1", BaseSignaling.DEFAULT_PORT);
        } catch (final UnknownHostException e) {
            e.printStackTrace();
        }
        final RtcClient client = new RtcClient(
                clientSideSignaling,
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
                    public void onVideoFrame(final VideoFrame frame) {
                        // TODO: show frame
                    }
                }
        );

        new Thread(server::start).start();
        new Thread(client::start).start();
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
