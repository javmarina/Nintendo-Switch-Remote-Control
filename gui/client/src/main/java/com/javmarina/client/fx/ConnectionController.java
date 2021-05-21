package com.javmarina.client.fx;

import dev.onvoid.webrtc.RTCStats;
import dev.onvoid.webrtc.RTCStatsReport;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class ConnectionController {

    @FXML
    private Button closeButton;
    @FXML
    private LineChart<Number, Integer> chart;
    @FXML
    private Label framerate;
    @FXML
    private Label stats;
    @FXML
    public Pane container;
    @FXML
    private ImageView frames;

    private static final int MAX_ITEMS = 10;

    XYChart.Series<Number, Integer> series = new XYChart.Series<>();

    @FXML
    private void initialize() {
        chart.getData().add(series);
        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setForceZeroInRange(false);
    }

    public void setButtonListener(final Runnable runnable) {
        closeButton.setOnAction(event -> runnable.run());
    }

    public void setButtonEnabled(final boolean enabled) {
        closeButton.setDisable(!enabled);
    }

    public void setFramerateValue(final double framerateValue) {
        framerate.setText(String.format("%.2f fps", framerateValue));
    }

    public void newRtt(final int rtt) {
        // First remove, then add
        Platform.runLater(() -> {
            if (series.getData().size() == MAX_ITEMS) {
                series.getData().remove(0);
            }
            final int lastXValue;
            final int length = series.getData().size();
            if (length == 0) {
                lastXValue = -1;
            } else {
                lastXValue = series.getData().get(length-1).getXValue().intValue();
            }
            series.getData().add(new XYChart.Data<>(lastXValue+1, rtt));
        });
    }

    public void newImage(final Image image) {
        frames.setImage(image);
    }

    public void newStats(final RTCStatsReport report) {
        final Map<String, String> displayInfo = new HashMap<>(10);

        final Map<String, RTCStats> map = report.getStats();
        final String videoStreamKey = map.keySet().stream()
                .filter(s -> s.startsWith("RTCInboundRTPVideoStream_"))
                .findFirst().orElse(null);
        if (videoStreamKey != null) {
            final RTCStats videoStats = map.get(videoStreamKey);
            // https://www.w3.org/TR/webrtc-stats/#dom-rtcinboundrtpstreamstats
            // timestamp, type: INBOUND_RTP, qpSum, decoderImplementation,
            // lastPacketReceivedTimestamp, transportId, kind: "video",
            // trackId: "RTCMediaStreamTrack_receiver_2", ssrc, isRemote,
            // ackCount, mediaType: "video", headerBytesReceived, codecId,
            // bytesReceived, firCount, packetsReceived, pliCount, packetsLost,
            // keyFramesDecoded, totalDecodeTime, framesDecoded, totalSquaredInterFrameDelay,
            // totalInterFrameDelay
            if (videoStats.getMembers().containsKey("codecId")) {
                final RTCStats codecStats = map.get(videoStats.getMembers().get("codecId"));
                final String codec = (String) codecStats.getMembers().get("mimeType");
                final long clockRate = (long) codecStats.getMembers().get("clockRate");
                displayInfo.put("Video codec", String.format("%s (%d kHz)", codec, clockRate/1000));
            }
            displayInfo.put("Frames decoded", videoStats.getMembers().get("framesDecoded").toString());
            if (videoStats.getMembers().containsKey("framesPerSecond")) {
                displayInfo.put("FPS", videoStats.getMembers().get("framesPerSecond").toString());
            }
        }

        final String audioStreamKey = map.keySet().stream()
                .filter(s -> s.startsWith("RTCInboundRTPAudioStream_"))
                .findFirst().orElse(null);
        if (audioStreamKey != null) {
            final RTCStats audioStats = map.get(audioStreamKey);
            // https://www.w3.org/TR/webrtc-stats/#dom-rtcinboundrtpstreamstats
            // timestamp, type: INBOUND_RTP, qpSum, decoderImplementation,
            // lastPacketReceivedTimestamp, transportId, kind: "video",
            // trackId: "RTCMediaStreamTrack_receiver_2", ssrc, isRemote,
            // ackCount, mediaType: "video", headerBytesReceived, codecId,
            // bytesReceived, firCount, packetsReceived, pliCount, packetsLost,
            // eyFramesDecoded, totalDecodeTime, framesDecoded, totalSquaredInterFrameDelay,
            // totalInterFrameDelay
        }

        final String dataChannelKey = map.keySet().stream()
                .filter(s -> s.startsWith("RTCDataChannel_"))
                .findFirst().orElse(null);
        if (dataChannelKey != null) {
            final RTCStats dataChannelStats = map.get(dataChannelKey);
            // timestamp, type: DATA_CHANNEL, protocol = null,
            // bytesReceived, messagesSent, messagesReceived, datachannelid,
            // label, state, bytesSent
        }

        final String transportKey = map.keySet().stream()
                .filter(s -> s.startsWith("RTCTransport_"))
                .findFirst().orElse(null);
        if (transportKey != null) {
            final RTCStats transportStats = map.get(transportKey);
            // timestamp, type: TRANSPORT, bytesReceived,
            // dtlsState, localCertificateId, tlsVersion,
            // selectedCandidatePairChanges, bytesSent, selectedCandidatePairId,
            // dtlsCipher, srtpCipher, remoteCertificateId
            displayInfo.put("Bytes sent", transportStats.getMembers().get("bytesSent").toString());
        }

        final String streamKey = map.keySet().stream()
                .filter(s -> s.startsWith("RTCMediaStream_"))
                .findFirst().orElse(null);
        if (streamKey != null) {
            final RTCStats streamStats = map.get(streamKey);
            final String[] trackIds = (String[]) streamStats.getMembers().get("trackIds");
            for (final String trackId : trackIds) {
                final RTCStats trackStats = map.get(trackId);
                // timestamp, type: TRACK, detached,
                // kind ("audio" o "video"), ended, remoteSource, trackIdentifier,
                // mediaSourceId
                final String kind = (String) trackStats.getMembers().get("kind");
                final boolean remote = (boolean) trackStats.getMembers().get("remoteSource");
                if (kind.equals(MediaStreamTrack.VIDEO_TRACK_KIND) && remote) {
                    final long width = (long) trackStats.getMembers().get("frameWidth");
                    final long height = (long) trackStats.getMembers().get("frameHeight");
                    displayInfo.put("Frame size", String.format("%d x %d", width, height));
                }
            }
        }

        Platform.runLater(() -> stats.setText(displayInfo.entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\r\n"))
        ));
    }
}
