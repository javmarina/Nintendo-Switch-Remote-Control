package com.javmarina.webrtc;

import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;

import java.util.List;


// https://webrtc.org/getting-started/peer-connections-advanced
// https://github.com/devopvoid/webrtc-java/blob/master/webrtc-demo/webrtc-demo-api/src/main/java/dev/onvoid/webrtc/demo/net/PeerConnectionClient.java
// https://w3c.github.io/webrtc-pc/
// https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Connectivity
public class WebRtcPlayground {

    public static void main(final String... args) {
        WebRtcLoader.loadLibrary();
        testDevices();
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
