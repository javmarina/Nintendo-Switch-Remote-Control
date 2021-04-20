package com.javmarina.webrtc;

import dev.onvoid.webrtc.media.video.VideoCaptureCapability;

import java.util.List;


public final class VideoCapabilitySelection {

    enum Policy {
        BEST_FRAMERATE,
        BEST_RESOLUTION
    }

    public static VideoCaptureCapability selectCapability(final List<VideoCaptureCapability> capabilities,
                                                          final Policy selectionPolicy) {
        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException("No capabilities available");
        }
        VideoCaptureCapability bestCapability = null;
        for (final VideoCaptureCapability capability : capabilities) {
            if (bestCapability == null) {
                bestCapability = capability;
            } else {
                switch (selectionPolicy) {
                    case BEST_FRAMERATE:
                        final int bestFrameRate = bestCapability.frameRate;
                        final int newFrameRate = capability.frameRate;
                        if (newFrameRate > bestFrameRate) {
                            bestCapability = capability;
                        } else if (newFrameRate == bestFrameRate) {
                            if (VideoCapabilitySelection.getNumberOfPixels(capability)
                                    > VideoCapabilitySelection.getNumberOfPixels(bestCapability)) {
                                bestCapability = capability;
                            }
                        }
                        break;
                    case BEST_RESOLUTION:
                        final int bestPixels = VideoCapabilitySelection.getNumberOfPixels(bestCapability);
                        final int newPixels = VideoCapabilitySelection.getNumberOfPixels(capability);
                        if (newPixels > bestPixels) {
                            bestCapability = capability;
                        } else if (newPixels == bestPixels) {
                            if (capability.frameRate > bestCapability.frameRate) {
                                bestCapability = capability;
                            }
                        }
                        break;
                }
            }
        }
        return bestCapability;
    }

    private static int getNumberOfPixels(final VideoCaptureCapability capability) {
        return capability.height * capability.width;
    }
}
