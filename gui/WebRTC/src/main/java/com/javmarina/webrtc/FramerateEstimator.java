package com.javmarina.webrtc;

import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrackSink;


public class FramerateEstimator implements VideoTrackSink {

    private long lastTimestamp = -1;

    @Override
    public void onVideoFrame(final VideoFrame frame) {
        if (lastTimestamp >= 0) {
            final double framerate = 1000000000.0 / (frame.timestampNs - lastTimestamp);
            System.out.println(framerate);
        }
        lastTimestamp = frame.timestampNs;
    }
}
