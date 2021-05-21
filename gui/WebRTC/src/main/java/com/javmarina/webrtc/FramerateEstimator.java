package com.javmarina.webrtc;

import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrackSink;


public class FramerateEstimator implements VideoTrackSink {

    private final Callback callback;
    private long lastTimestamp = -1;

    public FramerateEstimator() {
        this(System.out::println);
    }

    public FramerateEstimator(final Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onVideoFrame(final VideoFrame frame) {
        if (lastTimestamp >= 0) {
            final double framerate = 1000000000.0 / (frame.timestampNs - lastTimestamp);
            callback.onNewFramerateValue(framerate);
        }
        lastTimestamp = frame.timestampNs;
    }

    public interface Callback {
        void onNewFramerateValue(double framerate);
    }
}
