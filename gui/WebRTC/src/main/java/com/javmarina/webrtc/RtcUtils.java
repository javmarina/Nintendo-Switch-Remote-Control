package com.javmarina.webrtc;

import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;

import java.util.List;
import java.util.concurrent.CompletableFuture;


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

    public static AudioDeviceModule getAudioDeviceModuleBlocking() {
        final CompletableFuture<AudioDeviceModule> future = new CompletableFuture<>();
        getAudioDeviceModule(future::complete);
        return future.join();
    }

    public static void getAudioCaptureDevices(final AudioDevicesCallback callback) {
        new Thread(() -> {
            final List<AudioDevice> list = MediaDevices.getAudioCaptureDevices();
            callback.onReady(list);
        }).start();
    }

    public static List<AudioDevice> getAudioCaptureDevicesBlocking() {
        final CompletableFuture<List<AudioDevice>> future = new CompletableFuture<>();
        getAudioCaptureDevices(future::complete);
        return future.join();
    }

    public static void getAudioRenderDevices(final AudioDevicesCallback callback) {
        new Thread(() -> {
            final List<AudioDevice> list = MediaDevices.getAudioRenderDevices();
            callback.onReady(list);
        }).start();
    }

    public static List<AudioDevice> getAudioRenderDevicesBlocking() {
        final CompletableFuture<List<AudioDevice>> future = new CompletableFuture<>();
        getAudioRenderDevices(future::complete);
        return future.join();
    }
}
