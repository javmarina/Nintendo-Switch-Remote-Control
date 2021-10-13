package com.javmarina.webrtc;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCRtpCodecCapability;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.media.MediaType;
import net.sourceforge.jsdp.Attribute;
import net.sourceforge.jsdp.Media;
import net.sourceforge.jsdp.MediaDescription;
import net.sourceforge.jsdp.SDPException;
import net.sourceforge.jsdp.SDPFactory;
import net.sourceforge.jsdp.SessionDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


public final class SdpUtils {

    public static RTCSessionDescription setCodecPreference(final RTCSessionDescription sessionDescription,
                                                           final CodecPreference codecPreference) {
        return new RTCSessionDescription(
                sessionDescription.sdpType,
                setCodecPreference(sessionDescription.sdp, codecPreference)
        );
    }

    private static Comparator<CodecInfo> getCodecComparator(final CodecPreference codecPreference) {
        return (o1, o2) -> {
            if (o1.name.contains(codecPreference.name)) {
                if (o2.name.contains(codecPreference.name)) {
                    return codecPreference.comparator.compare(o1, o2);
                } else {
                    return -1;
                }
            }
            if (o2.name.contains(codecPreference.name)) {
                return 1;
            }
            return 0;
        };
    }

    public static String setCodecPreference(final String sdp, final CodecPreference codecPreference) {
        try {
            final SessionDescription sessionDescription = SDPFactory.parseSessionDescription(sdp);
            final MediaDescription[] mediaDescriptions = sessionDescription.getMediaDescriptions();
            MediaDescription videoDescription = null;
            for (final MediaDescription mediaDescription : mediaDescriptions) {
                final Media media = mediaDescription.getMedia();
                if (media.getMediaType().equals("video")) {
                    videoDescription = mediaDescription;
                    break;
                }
            }

            if (videoDescription == null) {
                return sdp;
            }

            final String[] formats = videoDescription.getMedia().getMediaFormats();

            final Attribute[] rtpMapAttrs = videoDescription.getAttributes("rtpmap");
            final HashMap<String, String> rtpMap = new HashMap<>(rtpMapAttrs.length);
            for (final Attribute attr : rtpMapAttrs) {
                final String[] split = attr.getValue().split(" ");
                rtpMap.put(split[0], split[1]);
            }

            final Attribute[] fmtp = videoDescription.getAttributes("fmtp");
            final HashMap<String, String> fmtpMap = new HashMap<>(fmtp.length);
            for (final Attribute attr : fmtp) {
                final String[] split = attr.getValue().split(" ");
                fmtpMap.put(split[0], split[1]);
            }

            final List<CodecInfo> codecs = new ArrayList<>(formats.length);
            for (final String format : formats) {
                codecs.add(new CodecInfo(
                        format,
                        rtpMap.get(format),
                        fmtpMap.getOrDefault(format, null)
                ));
            }
            codecs.sort(getCodecComparator(codecPreference));

            videoDescription.getMedia().setMediaFormats(codecs.stream()
                    .map(codecInfo -> codecInfo.format)
                    .toArray(String[]::new)
            );
            return sessionDescription.toString();
        } catch (final SDPException e) {
            return sdp;
        }
    }

    private static final class CodecInfo {

        private final String format;
        private final String name;
        private final String attr;

        private CodecInfo(final String format, final String name, final String attr) {
            this.format = format;
            this.name = name;
            this.attr = attr;
        }
    }

    public enum CodecPreference {
        // The order in which values are declared is the order in which it will be presented to the user
        H264("H264", (o1, o2) -> 0),
        VP8("VP8", (o1, o2) -> 0),
        VP9("VP9", (o1, o2) -> {
            final int profile1 = Integer.parseInt(o1.attr.split("profile-id=")[1].substring(0, 1));
            final int profile2 = Integer.parseInt(o2.attr.split("profile-id=")[1].substring(0, 1));
            return -Integer.compare(profile1, profile2);
        }),
        AV1("AV1", (o1, o2) -> 0);

        private final String name;
        private final Comparator<CodecInfo> comparator;

        CodecPreference(final String name, final Comparator<CodecInfo> comparator) {
            this.name = name;
            this.comparator = comparator;
        }

        public static List<CodecPreference> getAvailablePreferences() {
            final List<RTCRtpCodecCapability> codecCapabilities =
                    new PeerConnectionFactory().getRtpReceiverCapabilities(MediaType.VIDEO).getCodecs();
            return Arrays.stream(CodecPreference.values())
                    .filter(codecPreference ->
                            codecCapabilities.stream().anyMatch(rtcRtpCodecCapability ->
                                    rtcRtpCodecCapability.getName().contains(codecPreference.name)))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
