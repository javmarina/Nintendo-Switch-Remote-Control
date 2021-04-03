package com.javmarina.server;

import com.fazecast.jSerialComm.SerialPort;
import com.javmarina.util.GeneralUtils;
import com.javmarina.util.Packet;
import com.javmarina.util.network.BaseConnection;
import com.javmarina.util.network.ServerConnection;
import com.javmarina.util.network.protocol.ServerProtocol;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.util.prefs.Preferences;


public final class Server {

    private static final int DEFAULT_BAUDRATE = 1000000; // 1 Mbps
    private static final String KEY_BAUDRATE = "key_baudrate";
    private static final String KEY_SERVER_PORT = "key_port";

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
        final JTextField jPort = new JTextField(15);
        final JTextField jBaudrate = new JTextField(5);
        final JLabel jPortLabel = new JLabel("Port", SwingConstants.RIGHT);
        final JLabel jBaudrateLabel = new JLabel("Baud rate", SwingConstants.RIGHT);

        // adjust size and set layout
        jPanel.setPreferredSize(new Dimension(200, 200));
        jPanel.setLayout(null);

        jPort.setText(prefs.get(KEY_SERVER_PORT, String.valueOf(BaseConnection.DEFAULT_PORT)));
        jBaudrate.setText(prefs.get(KEY_BAUDRATE, String.valueOf(DEFAULT_BAUDRATE)));

        final SerialPort[] ports = SerialPort.getCommPorts();
        final JComboBox<SerialPort> jComboBox = new JComboBox<>(ports);

        // add components
        jPanel.add(jButton);
        jPanel.add(jPort);
        jPanel.add(jPortLabel);
        jPanel.add(jBaudrate);
        jPanel.add(jBaudrateLabel);
        jPanel.add(jComboBox);

        // set component bounds (only needed by Absolute Positioning)
        jPortLabel.setBounds (10, 10, 80, 30);
        jPort.setBounds (110, 10, 80, 30);
        jBaudrateLabel.setBounds(10, 60, 80, 30);
        jBaudrate.setBounds(110, 60, 80, 30);
        jComboBox.setBounds(50, 110, 100, 30);
        jButton.setBounds (50, 160, 100, 30);

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
            final String port = jPort.getText();
            if (port.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Port cannot be empty");
                return;
            }
            if (GeneralUtils.isStringInteger(port)) {
                final int portNumber = Integer.parseInt(port);
                if (portNumber <= 1023 || portNumber > 65535) {
                    JOptionPane.showMessageDialog(frame, "Port must be in the range 1024-65535");
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Port must be numeric");
                return;
            }
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
            prefs.put(KEY_SERVER_PORT, port);
            prefs.put(KEY_BAUDRATE, baudrate);

            final SerialPort serialPort = (SerialPort) jComboBox.getSelectedItem();
            frame.setVisible(false);
            showConnectionFrame(
                    new SerialAdapter(serialPort, Integer.parseInt(baudrate)),
                    Integer.parseInt(port)
            );
        });
    }

    private static void showConnectionFrame(final SerialAdapter serialAdapter, final int serverPort) {
        final ConnectionFrame frame = new ConnectionFrame("Server");
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

        // Wait for client command
        final ServerConnection server;
        try {
            server = BaseConnection.newServerConnection(serverPort);
            final ServerProtocol serverProtocol = new ServerProtocol(
                    server,
                    new ServerProtocol.Callback() {
                        @Override
                        public void onPacketReceived(final Packet packet) {
                            final String message = GeneralUtils.byteArrayToString(packet.getBuffer());
                            System.out.println("From client: " + message);
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
                    });
            serverProtocol.waitForClientAsync();
        } catch (final SocketException e) {
            JOptionPane.showMessageDialog(frame, "Couldn't open socket. Select another port");
            e.printStackTrace();
            frame.setVisible(false);
            showInitialFrame();
        }
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
}
