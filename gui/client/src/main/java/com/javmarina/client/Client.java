package com.javmarina.client;

import com.javmarina.client.services.ControllerService;
import com.javmarina.client.services.DefaultJamepadService;
import com.javmarina.client.services.KeyboardService;
import com.javmarina.client.services.bot.DiscordService;
import com.javmarina.util.StoppableLoop;
import com.javmarina.webrtc.FramerateEstimator;
import com.javmarina.webrtc.RtcClient;
import com.javmarina.webrtc.WebRtcLoader;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.RTCStats;
import dev.onvoid.webrtc.RTCStatsReport;
import dev.onvoid.webrtc.media.Device;
import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;
import org.jetbrains.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;


public final class Client {

    private static final List<AudioDevice> AUDIO_DEVICES;

    static {
        WebRtcLoader.loadLibrary();
        AUDIO_DEVICES = MediaDevices.getAudioRenderDevices();
    }

    public static void main(final String... arg) {
        //noinspection OverlyBroadCatchBlock
        try {
            // Use system look & feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            e.printStackTrace();
        }

        showInitialFrame();
    }

    private static void showInitialFrame() {

        // construct components
        final JPanel jPanel = new JPanel();
        final JButton jButton = new JButton("Save settings");
        final JTextField jSessionId = new JTextField(4);
        final JLabel jSessionIdLabel = new JLabel("Session ID", SwingConstants.RIGHT);

        // adjust size and set layout
        jPanel.setPreferredSize(new Dimension(200, 200));
        jPanel.setLayout(null);

        final ArrayList<ControllerService> services = getAvailableServices();
        final int totalSize = services.size();
        final JComboBox<ControllerService> jComboBox =
                new JComboBox<>(services.toArray(new ControllerService[totalSize]));

        final String[] audioDeviceNames = AUDIO_DEVICES.stream().map(Device::getName).toArray(String[]::new);
        final JComboBox<String> jAudioComboBox = new JComboBox<>(audioDeviceNames);

        // add components
        jPanel.add(jButton);
        jPanel.add(jSessionId);
        jPanel.add(jSessionIdLabel);
        jPanel.add(jComboBox);
        jPanel.add(jAudioComboBox);

        // set component bounds (only needed by Absolute Positioning)
        jSessionIdLabel.setBounds(10, 10, 80, 30);
        jSessionId.setBounds(110, 10, 80, 30);
        jComboBox.setBounds(10, 60, 180, 30);
        jAudioComboBox.setBounds(10, 110, 180, 30);
        jButton.setBounds(50, 160, 100, 30);

        final JFrame frame = new JFrame("Client configuration");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(jPanel);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(
                dim.width/2-frame.getSize().width,
                dim.height/2-frame.getSize().height/2
        );

        jButton.addActionListener(actionEvent -> {
            final String sessionId = jSessionId.getText();
            if (!SessionId.validateString(sessionId)) {
                JOptionPane.showMessageDialog(frame, "Invalid session ID");
                return;
            }
            if (jComboBox.getSelectedIndex() == -1) {
                JOptionPane.showMessageDialog(frame, "You must select a controller");
                return;
            }

            final ControllerService service = (ControllerService) jComboBox.getSelectedItem();
            final AudioDevice audioDevice = AUDIO_DEVICES.get(jAudioComboBox.getSelectedIndex());
            showConnectionFrame(service, SessionId.fromString(sessionId), audioDevice);
            frame.setVisible(false);
        });
    }

    /**
     * Gets a list of available {@link ControllerService}. Keyboard and Discord bot are always
     * available. Additional {@link DefaultJamepadService} are added if there are connected controllers.
     * @return list of available services.
     */
    private static ArrayList<ControllerService> getAvailableServices() {
        final ArrayList<DefaultJamepadService> jamepadServiceList = JamepadManager.getAvailableJamepadServices();
        final ArrayList<ControllerService> allServices = new ArrayList<>(2 + jamepadServiceList.size());
        allServices.add(new KeyboardService());
        final String token = getDiscordToken();
        if (token != null) {
            allServices.add(new DiscordService(token));
        }
        allServices.addAll(jamepadServiceList);

        return allServices;
    }

    /**
     * Create a 'discord.properties' file inside /resources with a field called DiscordBotToken
     * @return the Discord token or null if not found
     */
    @Nullable
    private static String getDiscordToken() {
        try (final InputStream input
                     = Client.class.getClassLoader().getResourceAsStream("discord.properties")) {

            final Properties prop = new Properties();
            if (input == null) {
                // Unable to find discord.properties
                return null;
            }

            // load a properties file from class path, inside static method
            prop.load(input);

            //get the property value and print it out
            return prop.getProperty("DiscordBotToken", null);
        } catch (final IOException ex) {
            return null;
        }
    }

    private static void showConnectionFrame(final ControllerService service,
                                            final SessionId sessionId,
                                            final AudioDevice audioDevice) {
        final ConnectionFrame connectionFrame =
                new ConnectionFrame("Client", service, sessionId, audioDevice);
        connectionFrame.setExtendedState(connectionFrame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        connectionFrame.setVisible(true);
    }

    private static class ConnectionFrame extends JFrame {

        private final JButton jButton;
        private final JLabel jLabel;
        private final JLabel jImage;
        private final JLabel jStats;
        private final DelayGraphPanel delayGraphPanel;

        private ConnectionFrame(final String title, final ControllerService service,
                                final SessionId sessionId, final AudioDevice audioDevice) {
            super(title);

            // construct components
            final JPanel jPanel = new JPanel();
            jButton = new JButton("Start");
            jLabel = new JLabel("Press start when server is ready", SwingConstants.CENTER);
            delayGraphPanel = new DelayGraphPanel();
            jImage = new JLabel();
            jStats = new JLabel();

            // add action listener to button
            jButton.addActionListener(new ConnectionFrame.ButtonListener(
                    service,
                    this,
                    sessionId,
                    audioDevice
            ));

            // adjust size and set layout
            jPanel.setPreferredSize(new Dimension(540, 360));
            jPanel.setLayout(null);

            // add components
            jPanel.add(jButton);
            jPanel.add(jLabel);
            jPanel.add(jImage);
            jPanel.add(jStats);
            jPanel.add(delayGraphPanel);

            // set component bounds (only needed by Absolute Positioning)
            jButton.setBounds(55, 45, 100, 20);
            jLabel.setBounds(10, 10, 200, 25);
            delayGraphPanel.setBounds(10,80,200,260);
            jImage.setBounds(220, 10, 240, 320);
            jStats.setBounds(10, 320, 200, 200);

            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            getContentPane().add(jPanel);
            pack();
        }

        private void newStats(final RTCStatsReport report) {
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

            jStats.setText("<html>" + displayInfo.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("<br>"))
            );
        }

        private static class ButtonListener implements ActionListener {

            private boolean started;

            private final RtcClient rtcClient;

            private ButtonListener(final ControllerService service, final ConnectionFrame frame,
                                   final SessionId sessionId, final AudioDevice audioDevice) {
                final FrameProcessing frameProcessing = new FrameProcessing(
                        () -> frame.getWidth() - 240,
                        imageIcon -> SwingUtilities.invokeLater(() -> {
                            frame.jImage.setBounds(220, 10, imageIcon.getIconWidth(), imageIcon.getIconHeight());
                            frame.jImage.setIcon(imageIcon);
                        })
                );
                final Thread thread = new Thread(frameProcessing);

                final ActionListener taskPerformer = new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent evt) {
                        rtcClient.getStats(report ->
                                SwingUtilities.invokeLater(()
                                        -> frame.newStats(report)));
                    }
                };
                final Timer timer = new Timer(1000, taskPerformer);

                final AudioDeviceModule deviceModule = new AudioDeviceModule();
                deviceModule.setPlayoutDevice(audioDevice);
                // TODO: this doesn't currently work, defaults to speaker

                this.rtcClient = new RtcClient(
                        sessionId,
                        service::getControllerStatus,
                        deviceModule,
                        new RtcClient.Callback() {
                            @Override
                            public void onRttReplyReceived(final int milliseconds) {
                                frame.jLabel.setText(String.format("RTT: %d ms", milliseconds));
                                frame.delayGraphPanel.addDelay(milliseconds);
                            }

                            @Override
                            public void onSessionStarted() {
                                started = true;
                                service.start();
                                thread.start();
                                timer.start();
                                frame.jButton.setText("Exit");
                            }

                            @Override
                            public void onSessionStopped() {
                                service.finish();
                                frameProcessing.stop(() -> {
                                    timer.stop();
                                    frame.setVisible(false);
                                    showInitialFrame();
                                });
                            }

                            @Override
                            public void onInvalidSessionId() {
                                JOptionPane.showMessageDialog(frame, "Invalid session ID");
                                frame.setVisible(false);
                                showInitialFrame();
                            }

                            @Override
                            public void onVideoFrame(final VideoFrame videoFrame) {
                                frameProcessing.newFrame(videoFrame);
                            }
                        }
                );
            }

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (started) {
                    // "Exit" button clicked
                    started = false;
                    System.out.println("Stopping session");
                    rtcClient.stop();
                } else {
                    // "Start" button clicked
                    try {
                        rtcClient.start(); // onSessionStarted() will be called if successful
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static final class FrameProcessing extends StoppableLoop {

        private final Object lock = new Object();
        private final WidthProvider widthProvider;
        private final Callback callback;
        private final FramerateEstimator framerateEstimator = new FramerateEstimator();

        @Nullable
        private VideoFrame nextFrame = null;
        private ByteBuffer byteBuffer;
        private int lastWidth = -1;
        private int lastHeight = -1;

        private FrameProcessing(final WidthProvider widthProvider, final Callback callback) {
            this.widthProvider = widthProvider;
            this.callback = callback;
        }

        @Override
        public void loop() {
            final VideoFrame currentFrame;
            synchronized (lock) {
                currentFrame = nextFrame;
                nextFrame = null;
            }
            if (currentFrame != null) {
                final VideoFrameBuffer buffer = currentFrame.buffer;
                final int width = buffer.getWidth();
                final int height = buffer.getHeight();

                if (byteBuffer == null || lastHeight != height || lastWidth != width) {
                    byteBuffer = ByteBuffer.allocate(width * height * 4);
                }

                try {
                    VideoBufferConverter.convertFromI420(buffer, byteBuffer, FourCC.ARGB);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final byte[] bytes = byteBuffer.array();
                final DataBufferByte dataBuffer = new DataBufferByte(bytes, bytes.length);
                final ColorModel cm = new ComponentColorModel(
                        ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
                        new int[]{8, 8, 8, 8},
                        true,
                        false,
                        Transparency.TRANSLUCENT,
                        DataBuffer.TYPE_BYTE
                );
                final BufferedImage image = new BufferedImage(
                        cm,
                        Raster.createInterleavedRaster(
                                dataBuffer,
                                width,
                                height,
                                width * 4,
                                4,
                                new int[]{2, 1, 0, 3},
                                null),
                        false,
                        null
                );

                final int targetWidth = widthProvider.getFrameWidth();
                final int targetHeight = (int) (targetWidth * ((double) height / width));

                final ImageIcon imageIcon = new ImageIcon(
                        image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_FAST)
                );
                callback.onImageIcon(imageIcon);

                framerateEstimator.onVideoFrame(currentFrame);

                currentFrame.release();

                lastWidth = width;
                lastHeight = height;
            }
        }

        private void newFrame(final VideoFrame frame) {
            synchronized (lock) {
                if (nextFrame != null) {
                    nextFrame.release();
                }
                frame.retain();
                nextFrame = frame;
            }
        }

        private interface Callback {
            void onImageIcon(final ImageIcon imageIcon);
        }

        private interface WidthProvider {
            int getFrameWidth();
        }
    }
}
