package com.javmarina.server;

import com.fazecast.jSerialComm.SerialPort;
import com.javmarina.util.Controller;
import com.javmarina.util.GeneralUtils;
import com.javmarina.util.UdpUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.prefs.Preferences;


public final class Server {

    private static final int DEFAULT_BAUDRATE = 1000000; // 1 Mbps
    private static final String KEY_BAUDRATE = "key_baudrate";
    private static final String KEY_SERVER_PORT = "key_port";

    private static final int FPS = 60;

    private static SendThread sendThread;

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

        jPort.setText(prefs.get(KEY_SERVER_PORT, String.valueOf(UdpUtils.DEFAULT_PORT)));
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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

        new Thread(new Runnable() {

            DatagramSocket socket;
            InetAddress address;
            int clientPort;

            @Override
            public void run() {
                // Wait for client command
                try {
                    // While we could use UdpUtils methods, we need to discover the client's IP
                    // address and port
                    socket = new DatagramSocket(serverPort);
                    final byte[] buf = new byte[1];
                    final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    assert buf[0] == UdpUtils.START;

                    address = packet.getAddress();
                    clientPort = packet.getPort();

                    UdpUtils.sendSingleByte(socket, address, clientPort, UdpUtils.START_ACK);

                    sendThread = new SendThread(socket, address, clientPort);
                    sendThread.start();

                    final Thread input = new ReceiveThread(socket, address, clientPort, frame, serialAdapter);
                    input.start();

                    Thread.currentThread().interrupt();
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final IOException e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage());
                    serialAdapter.closePort();
                    frame.setVisible(false);
                    showInitialFrame();
                }
            }
        }).start();
    }

    private static class ConnectionFrame extends JFrame {

        private final JLabel jConnectionLabel;
        private final JLabel jSerialLabel;
        private final ControllerPanel controllerPanel;
        private final JButton connectionLostButton;

        private ConnectionFrame(final String title) {
            super(title);

            final JPanel jPanel = new JPanel();
            // Adjust size and set layout
            jPanel.setPreferredSize(new Dimension(500, 350));
            //jPanel.setLayout(null);

            controllerPanel = new ControllerPanel();
            //controllerPanel.setBounds(10,10,500,300);

            connectionLostButton = new JButton("Simulate lost connection");
            //connectionLostButton.setBounds(15, 310, 150, 30);

            jConnectionLabel = new JLabel("Waiting for the client request");
            jSerialLabel = new JLabel("Searching for serial adapter");

            // Add components
            jPanel.add(controllerPanel);
            jPanel.add(jSerialLabel);
            jPanel.add(jConnectionLabel);
            jPanel.add(connectionLostButton);

            // Set component bounds (only needed by Absolute Positioning)
            //jConnectionLabel.setBounds(50, 350, 300, 30);
            //jSerialLabel.setBounds(50, 390, 300, 40);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getContentPane().add(jPanel);
            pack();
        }

        private void setSerialInfo(final String text) {
            jSerialLabel.setText(text);
        }

        private void setConnectionInfo(final String text) {
            jConnectionLabel.setText(text);
        }

        private void updateControllerUi(final byte[] packet) {
            controllerPanel.updateUi(packet);
        }
    }

    private static class ReceiveThread extends Thread {

        private final DatagramSocket socket;
        private final InetAddress address;
        private final int clientPort;
        private final ConnectionFrame connectionFrame;
        private final SerialAdapter serialAdapter;

        private boolean running;
        private int crcFailCount = 0;

        private ReceiveThread(final DatagramSocket socket, final InetAddress address,
                              final int clientPort, final ConnectionFrame connectionFrame,
                              final SerialAdapter serialAdapter) {
            this.socket = socket;
            this.address = address;
            this.clientPort = clientPort;
            this.connectionFrame = connectionFrame;
            this.serialAdapter = serialAdapter;
        }

        @Override
        public synchronized void start() {
            super.start();
            running = true;
        }

        @Override
        public void run() {
            final byte[] received = new byte[8];
            while (running) {
                try {
                    // Fill received buffer with contents from client
                    UdpUtils.receivePacket(socket, received);
                    final String message = GeneralUtils.byteArrayToString(received);
                    System.out.println("From client: " + message);
                    connectionFrame.setConnectionInfo("<html>From client: " + message + "<br>UDP CRC fails: " + crcFailCount + "</html>");
                    if (received[7] == UdpUtils.EXIT) {
                        running = false;
                        sendThread.interrupt();
                        GeneralUtils.sleep(400); // Let the threads interrupt correctly
                        UdpUtils.sendSingleByte(socket, address, clientPort, UdpUtils.EXIT_ACK);
                        System.out.println("Sending exit_ack");
                        break;
                    }
                    if (received[7] == UdpUtils.PING) {
                        final byte save = received[7];
                        received[7] = UdpUtils.PING_REPLY;
                        UdpUtils.sendPacket(socket, address, clientPort, received);
                        System.out.println("Sending ping_reply");
                        received[7] = save; // Leave it as before
                    }
                    if (received[7] == UdpUtils.NO_ACTION || received[7] == UdpUtils.PING) {
                        // Update UI
                        connectionFrame.updateControllerUi(received);
                        // Send to MCU
                        if (!connectionFrame.connectionLostButton.getModel().isPressed()) {
                            received[7] = Controller.VENDORSPEC;
                            final boolean result = serialAdapter.sendPacket(received);
                            connectionFrame.setSerialInfo(result ? "Synced!" : "Packet error");
                        }
                    }
                } catch (final UdpUtils.InvalidCrcException e) {
                    System.out.println("CRC is not valid");
                    crcFailCount++;
                } catch (final IOException e) {
                    System.out.println("Error while reading input from client");
                }
            }
            System.out.println("Closing socket server!!");
            socket.close();
            serialAdapter.closePort();
            connectionFrame.setVisible(false);
            showInitialFrame();
        }
    }

    private static class SendThread extends Thread {

        private final DatagramSocket socket;
        private final InetAddress address;
        private final int clientPort;

        private boolean running;

        private SendThread(final DatagramSocket socket, final InetAddress address,
                              final int clientPort) {
            this.socket = socket;
            this.address = address;
            this.clientPort = clientPort;
        }

        @Override
        public void run() {
            running = true;
            int i = 0;
            final ByteBuffer buffer = ByteBuffer.allocate(8);
            while (running) {
                try {
                    UdpUtils.sendPacket(socket, address, clientPort, buffer.putInt(0, i).array());
                    System.out.println("Sending frame " + i);
                    i++;
                    GeneralUtils.sleep(1000/FPS);
                } catch (final IOException e) {
                    System.out.println("Error while sending frames");
                }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            running = false;
        }
    }
}
