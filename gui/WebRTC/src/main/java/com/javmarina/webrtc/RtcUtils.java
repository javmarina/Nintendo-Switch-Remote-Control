package com.javmarina.webrtc;

import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;

import java.util.List;


/**
 * Utility methods for WebRTC functions that can not be executed in the JavaFX thread.
 */
public final class RtcUtils {

    public interface AudioDeviceModuleCallback {
        void onCreated(final AudioDeviceModule audioDeviceModule);
    }

    public interface AudioDevicesCallback {
        void onReady(final List<AudioDevice> audioDevices);
    }

    public static void getAudioDeviceModule(final AudioDeviceModuleCallback callback) {
        new Thread(() -> {
            final AudioDeviceModule audioDeviceModule = new AudioDeviceModule();
            callback.onCreated(audioDeviceModule);
        }).start();
    }

    public static void getAudioCaptureDevices(final AudioDevicesCallback callback) {
        new Thread(() -> {
            final List<AudioDevice> list = MediaDevices.getAudioCaptureDevices();
            callback.onReady(list);
        }).start();
    }

    public static void getAudioRenderDevices(final AudioDevicesCallback callback) {
        new Thread(() -> {
            final List<AudioDevice> list = MediaDevices.getAudioRenderDevices();
            callback.onReady(list);
        }).start();
    }
}
