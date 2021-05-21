package com.javmarina.server;

import com.fazecast.jSerialComm.SerialPort;
import com.javmarina.util.GeneralUtils;
import com.javmarina.util.Packet;
import com.javmarina.webrtc.RtcServer;
import com.javmarina.webrtc.WebRtcLoader;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.media.Device;
import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;
import dev.onvoid.webrtc.media.video.VideoTrack;

import javax.swing.DefaultComboBoxModel;
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
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;


public final class Server {

    private static final int DEFAULT_BAUDRATE = 1000000; // 1 Mbps
    private static final String KEY_BAUDRATE = "key_baudrate";

    static {
        WebRtcLoader.loadLibrary();
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
        final Preferences prefs = Preferences.userNodeForPackage(Server.class);

        // construct components
        final JPanel jPanel = new JPanel();
        final JButton jButton = new JButton("Open server");
        final JTextField jSessionId = new JTextField(4);
        final JTextField jBaudrate = new JTextField(5);
        final JLabel jSessionIdLabel = new JLabel("Session ID", SwingConstants.RIGHT);
        final JLabel jBaudrateLabel = new JLabel("Baud rate", SwingConstants.RIGHT);
        final JLabel jImage = new JLabel();

        // adjust size and set layout
        jPanel.setPreferredSize(new Dimension(200+20+320, 360));
        jPanel.setLayout(null);

        final SessionId sessionId = new SessionId();
        jSessionId.setText(sessionId.toString());
        jSessionId.setEditable(false);
        jBaudrate.setText(prefs.get(KEY_BAUDRATE, String.valueOf(DEFAULT_BAUDRATE)));

        final SerialPort[] ports = SerialPort.getCommPorts();
        final SerialPort[] selectablePorts = new SerialPort[ports.length + 1];
        System.arraycopy(ports, 0, selectablePorts, 0, ports.length);
        final JComboBox<SerialPort> jComboBox = new JComboBox<>(selectablePorts);

        final JComboBox<String> jVideoCapabilityComboBox = new JComboBox<>(new String[0]);

        final PeerConnectionFactory factory = new PeerConnectionFactory();
        final VideoDevice[] currentVideoDevice = {null};
        final VideoDeviceSource videoDeviceSource = new VideoDeviceSource();
        final VideoCaptureCapability[] currentVideoCapability = {null};
        final VideoTrack[] videoTrack = {null};

        final List<VideoDevice> videoDevices = MediaDevices.getVideoCaptureDevices();
        final String[] videoDeviceNames = videoDevices.stream().map(Device::getName).toArray(String[]::new);
        final JComboBox<String> jVideoComboBox = new JComboBox<>(videoDeviceNames);
        jVideoComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentVideoDevice[0] = videoDevices.get(jVideoComboBox.getSelectedIndex());
                videoDeviceSource.setVideoCaptureDevice(currentVideoDevice[0]);
                updateVideoCapabilityComboBox(currentVideoDevice[0], jVideoCapabilityComboBox);
            }
        });

        jVideoCapabilityComboBox.addItemListener(e -> {
            final String capabilityName = (String) jVideoCapabilityComboBox.getSelectedItem();
            if (capabilityName == null) {
                return;
            }
            currentVideoCapability[0] = VideoCapabilityUtils.fromString(capabilityName);

            if (videoTrack[0] != null) {
                videoTrack[0].dispose();
            }
            videoDeviceSource.stop();
            videoDeviceSource.setVideoCaptureDevice(currentVideoDevice[0]);
            videoDeviceSource.setVideoCaptureCapability(currentVideoCapability[0]);

            videoTrack[0] = factory.createVideoTrack("videoTrack", videoDeviceSource);
            videoTrack[0].addSink(frame -> {
                frame.retain();
                final VideoFrameBuffer buffer = frame.buffer;
                final int width = buffer.getWidth();
                final int height = buffer.getHeight();

                final ByteBuffer byteBuffer = ByteBuffer.allocate(width * height * 4);

                try {
                    VideoBufferConverter.convertFromI420(buffer, byteBuffer, FourCC.ARGB);
                } catch (final Exception e1) {
                    e1.printStackTrace();
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

                final int targetWidth = 320;
                final int targetHeight = 240;

                final ImageIcon imageIcon = new ImageIcon(
                        image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT)
                );
                SwingUtilities.invokeLater(() -> jImage.setIcon(imageIcon));

                frame.release();
            });
            videoDeviceSource.start();
        });

        currentVideoDevice[0] = videoDevices.get(jVideoComboBox.getSelectedIndex());
        videoDeviceSource.setVideoCaptureDevice(currentVideoDevice[0]);
        updateVideoCapabilityComboBox(currentVideoDevice[0], jVideoCapabilityComboBox);

        final List<AudioDevice> audioDevices = MediaDevices.getAudioCaptureDevices();
        final String[] audioDeviceNames = audioDevices.stream().map(Device::getName).toArray(String[]::new);
        final JComboBox<String> jAudioComboBox = new JComboBox<>(audioDeviceNames);

        // add components
        jPanel.add(jButton);
        jPanel.add(jSessionId);
        jPanel.add(jSessionIdLabel);
        jPanel.add(jBaudrate);
        jPanel.add(jBaudrateLabel);
        jPanel.add(jComboBox);
        jPanel.add(jVideoComboBox);
        jPanel.add(jVideoCapabilityComboBox);
        jPanel.add(jAudioComboBox);
        jPanel.add(jImage);

        // set component bounds (only needed by Absolute Positioning)
        jSessionIdLabel.setBounds(10, 10, 80, 30);
        jSessionId.setBounds(110, 10, 80, 30);
        jBaudrateLabel.setBounds(10, 60, 80, 30);
        jBaudrate.setBounds(110, 60, 80, 30);
        jComboBox.setBounds(10, 110, 180, 30);
        jVideoComboBox.setBounds(10, 160, 180, 30);
        jVideoCapabilityComboBox.setBounds(10, 210, 180, 30);
        jAudioComboBox.setBounds(10, 260, 180, 30);
        jButton.setBounds(50, 310, 100, 30);
        jImage.setBounds(210, 10, 320, 240);

        final JFrame frame = new JFrame("Server configuration");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(jPanel);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(
                dim.width/2,
                dim.height/2-frame.getSize().height/2
        );

        jButton.addActionListener(actionEvent -> {
            final String baudrate = jBaudrate.getText();
            if (baudrate.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Baud rate cannot be empty");
                return;
            }
            if (GeneralUtils.isStringInteger(baudrate)) {
                final int baud = Integer.parseInt(baudrate);
                if (baud < 9600 || baud > 1000000) {
                    JOptionPane.showMessageDialog(frame, "Baud rate must be in the range 9600-1000000");
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Baud rate must be numeric");
                return;
            }
            if (jComboBox.getSelectedIndex() == -1) {
                JOptionPane.showMessageDialog(frame, "WARNING: No serial port selected");
            }
            // Save preferences
            prefs.put(KEY_BAUDRATE, baudrate);

            final SerialPort serialPort = (SerialPort) jComboBox.getSelectedItem();
            frame.setVisible(false);
            videoDeviceSource.stop();
            showConnectionFrame(
                    new SerialAdapter(serialPort, Integer.parseInt(baudrate)),
                    sessionId,
                    videoDeviceSource,
                    audioDevices.get(jAudioComboBox.getSelectedIndex())
            );
        });
    }

    private static void updateVideoCapabilityComboBox(final VideoDevice videoDevice,
                                                      final JComboBox<String> jVideoCapabilityComboBox) {
        final String[] capabilityNames = MediaDevices.getVideoCaptureCapabilities(videoDevice).stream()
                .distinct()
                .sorted(VideoCapabilityUtils.getComparator())
                .map(VideoCapabilityUtils::toString)
                .toArray(String[]::new);
        final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(capabilityNames);
        jVideoCapabilityComboBox.setModel(model);
        jVideoCapabilityComboBox.setSelectedItem(null);
        jVideoCapabilityComboBox.setSelectedIndex(0);
    }

    private static void showConnectionFrame(final SerialAdapter serialAdapter, final SessionId sessionId,
                                            final VideoDeviceSource videoDeviceSource,
                                            final AudioDevice audioDevice) {
        final ConnectionFrame frame = new ConnectionFrame(
                String.format("Server (session ID: %s)", sessionId.toString())
        );
        frame.setResizable(false);
        frame.setVisible(true);

        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(
                dim.width/2,
                dim.height/2-frame.getSize().height/2
        );

        try {
            frame.setSerialInfo("Trying to sync");
            serialAdapter.sync(true);
            frame.setSerialInfo("Synced!");
            System.out.println("Synced!");
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
            serialAdapter.closePort();
            frame.setVisible(false);
            showInitialFrame();
            return;
        }

        if (!serialAdapter.isFake()) {
            runSerialPortTests(serialAdapter, frame);
        }

        final AudioDeviceModule deviceModule = new AudioDeviceModule();
        deviceModule.setRecordingDevice(audioDevice);

        final RtcServer server = new RtcServer(
                sessionId,
                deviceModule,
                videoDeviceSource,
                new RtcServer.Callback() {
                    @Override
                    public void onPacketReceived(final Packet packet) {
                        final String message = GeneralUtils.byteArrayToString(packet.getBuffer());
                        frame.setConnectionInfo("From client: " + message);
                        // Update UI
                        frame.updateControllerUi(packet);
                        // Send to MCU
                        if (!frame.connectionLostButton.getModel().isPressed()) {
                            final boolean result = serialAdapter.sendPacket(packet);
                            frame.setSerialInfo(result ? "Synced!" : "Packet error");
                        }
                    }

                    @Override
                    public void onSessionStarted() {
                    }

                    @Override
                    public void onSessionStopped() {
                        System.out.println("Session stopped");
                        serialAdapter.closePort();
                        frame.setVisible(false);
                        showInitialFrame();
                    }

                    @Override
                    public void onError(final Exception e) {
                        JOptionPane.showMessageDialog(frame, e.getMessage());
                        serialAdapter.closePort();
                        frame.setVisible(false);
                        showInitialFrame();
                    }

                    @Override
                    public void onInvalidSessionId() {
                        JOptionPane.showMessageDialog(frame, "Invalid session ID");
                        frame.setVisible(false);
                        showInitialFrame();
                    }
                }
        );
        new Thread(() -> {
            try {
                server.start();
            } catch (final Exception e) {
                JOptionPane.showMessageDialog(frame, "An error occurred, please try again.");
                e.printStackTrace();
                frame.setVisible(false);
                showInitialFrame();
            }
        }).start();
    }

    private static void runSerialPortTests(final SerialAdapter serialAdapter, final ConnectionFrame frame) {
        final SerialAdapter.TestResults testResults = serialAdapter.testSpeed(100);
        final String msg;
        String url = null;
        switch (testResults.errorType) {
            case NONE:
                final String temp = String.format("Minimum: %d ms\r\nMaximum: %d ms\r\nAverage: %.3f ms\r\nError count: %d",
                        testResults.min, testResults.max, testResults.avg, testResults.errorCount);
                if (testResults.avg > 10.0) {
                    url = "https://projectgus.com/2011/10/notes-on-ftdi-latency-with-arduino/";
                    msg = temp + "\r\nAverage is high. You might need to adjust the latency timer of the FTDI adapter." +
                            "\r\nSee " + url + " for more info.";
                } else {
                    msg = temp;
                }
                break;
            case NO_ACKS:
                msg = "No packets were sent correctly";
                break;
            case SYNC_ERROR:
                msg = "Sync error, test aborted";
                break;
            default:
                msg = "Unknown error";
                break;
        }
        if (url != null) {
            final int selection = JOptionPane.showOptionDialog(
                    frame,
                    msg,
                    "",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{"OK", "Open"},
                    "OK");

            if (selection == 1) {
                try {
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (final IOException ignored) {
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, msg);
        }
    }

    private static class ConnectionFrame extends JFrame {

        private final JLabel jConnectionLabel;
        private final JLabel jSerialLabel;
        private final ControllerPanel controllerPanel;
        private final JButton connectionLostButton;

        private ConnectionFrame(final String title) {
            super(title);

            final JPanel jPanel = new JPanel();
            // Adjust size
            jPanel.setPreferredSize(new Dimension(500, 350));

            controllerPanel = new ControllerPanel();
            connectionLostButton = new JButton("Simulate lost connection");
            jConnectionLabel = new JLabel("Waiting for the client request");
            jSerialLabel = new JLabel("Searching for serial adapter");

            // Add components
            jPanel.add(controllerPanel);
            jPanel.add(jSerialLabel);
            jPanel.add(jConnectionLabel);
            jPanel.add(connectionLostButton);

            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            getContentPane().add(jPanel);
            pack();
        }

        private void setSerialInfo(final String text) {
            jSerialLabel.setText(text);
        }

        private void setConnectionInfo(final String text) {
            jConnectionLabel.setText(text);
        }

        private void updateControllerUi(final Packet packet) {
            controllerPanel.updateUi(packet);
        }
    }

    public static final class VideoCapabilityUtils {

        private static final String FORMAT = "%dx%d@%d";

        private static final Comparator<VideoCaptureCapability> comparator = (o1, o2) -> {
            final int compare1 = Integer.compare(
                    o1.width*o1.height,
                    o2.width*o2.height
            );
            if (compare1 == 0) {
                return Integer.compare(o1.frameRate, o2.frameRate);
            } else {
                return compare1;
            }
        };

        private static String toString(final VideoCaptureCapability capability) {
            return String.format(FORMAT, capability.width, capability.height, capability.frameRate);
        }

        private static VideoCaptureCapability fromString(final String name) {
            final String[] parts = name.split("@");
            final String[] parts2 = parts[0].split("x");
            final int width = Integer.parseInt(parts2[0]);
            final int height = Integer.parseInt(parts2[1]);
            final int framerate = Integer.parseInt(parts[1]);
            return new VideoCaptureCapability(width, height, framerate);
        }

        public static Comparator<VideoCaptureCapability> getComparator() {
            return comparator;
        }
    }
}
