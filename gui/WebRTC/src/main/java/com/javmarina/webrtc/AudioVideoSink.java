package com.javmarina.webrtc;

import dev.onvoid.webrtc.media.audio.AudioTrackSink;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrackSink;


public interface AudioVideoSink extends AudioTrackSink, VideoTrackSink {

    static AudioVideoSink fromSinks(final AudioTrackSink audioTrackSink,
                                    final VideoTrackSink videoTrackSink) {
        return new AudioVideoSink() {
            @Override
            public void onData(final byte[] data, final int bitsPerSample, final int sampleRate,
                               final int channels, final int frames) {
                audioTrackSink.onData(data, bitsPerSample, sampleRate, channels, frames);
            }

            @Override
            public void onVideoFrame(final VideoFrame frame) {
                videoTrackSink.onVideoFrame(frame);
            }
        };
    }

    @Override
    void onData(final byte[] data, final int bitsPerSample, final int sampleRate,
                final int channels, final int frames);

    @Override
    void onVideoFrame(final VideoFrame frame);
}
