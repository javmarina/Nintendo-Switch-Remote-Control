package com.javmarina.webrtc;

import dev.onvoid.webrtc.internal.NativeLoader;


public class WebRtcLoader {

    public static void loadLibrary() {
        try {
            NativeLoader.loadLibrary("webrtc-java");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
