package com.javmarina.client;

import com.javmarina.client.services.ControllerService;
import com.javmarina.client.services.DefaultJamepadService;
import com.javmarina.client.services.KeyboardService;
import com.javmarina.client.services.bot.DiscordService;
import com.javmarina.util.GeneralUtils;
import com.javmarina.util.UdpUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.prefs.Preferences;


public final class Client {

    // Timeout for establishing or ending a connection with server
    private static final int CONNECTION_TIMEOUT = 2000; // 2 seconds
    // Packet send rate
    private static final int PERIOD_MS = 5; // ms

    private static final String DEFAULT_ADDRESS = "localhost";
    private static final String KEY_ADDRESS = "key_address";
    private static final String KEY_PORT = "key_port";

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
        final Preferences prefs = Preferences.userNodeForPackage(Client.class);

        // construct components
        final JPanel jPanel = new JPanel();
        final JButton jButton = new JButton("Save settings");
        final JTextField jIp = new JTextField(15);
        final JTextField jPort = new JTextField(5);
        final JLabel jIpLabel = new JLabel("IP or domain", SwingConstants.RIGHT);
        final JLabel jPortLabel = new JLabel("Port", SwingConstants.RIGHT);

        // adjust size and set layout
        jPanel.setPreferredSize(new Dimension(200, 200));
        jPanel.setLayout(null);

        jIp.setText(prefs.get(KEY_ADDRESS, DEFAULT_ADDRESS));
        jPort.setText(prefs.get(KEY_PORT, String.valueOf(UdpUtils.DEFAULT_PORT)));

        final ArrayList<ControllerService> services = getAvailableServices();

        final int totalSize = services.size();
        System.out.println(totalSize + " service(s) available");
        for (int i = 0; i < totalSize; i++) {
            System.out.println(" " + i + ". " + services.get(i).toString());
        }
        final JComboBox<ControllerService> jComboBox =
                new JComboBox<>(services.toArray(new ControllerService[totalSize]));

        // add components
        jPanel.add(jButton);
        jPanel.add(jIp);
        jPanel.add(jIpLabel);
        jPanel.add(jPort);
        jPanel.add(jPortLabel);
        jPanel.add(jComboBox);

        // set component bounds (only needed by Absolute Positioning)
        jIpLabel.setBounds (10, 10, 80, 30);
        jIp.setBounds (110, 10, 80, 30);
        jPortLabel.setBounds(10, 60, 80, 30);
        jPort.setBounds(110, 60, 80, 30);
        jComboBox.setBounds(50, 110, 100, 30);
        jButton.setBounds (50, 160, 100, 30);

        final JFrame frame = new JFrame("Client configuration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
            final String ip = jIp.getText();
            if (ip.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "IP cannot be empty");
                return;
            }
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
            if (jComboBox.getSelectedIndex() == -1) {
                JOptionPane.showMessageDialog(frame, "You must select a controller");
                return;
            }
            // Save preferences
            prefs.put(KEY_ADDRESS, ip);
            prefs.put(KEY_PORT, port);

            final ControllerService service = (ControllerService) jComboBox.getSelectedItem();
            showConnectionFrame(ip, Integer.parseInt(port), service);
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

    private static void showConnectionFrame(final String ip, final int port,
                                            final ControllerService service) {
        final ConnectionFrame connectionFrame =
                new ConnectionFrame("Client (server " + ip + ':' + port + ')');
        connectionFrame.setResizable(false);
        connectionFrame.setVisible(true);
        connectionFrame.jButton.addActionListener(new StartButtonListener(service, connectionFrame, ip, port));

        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        connectionFrame.setLocation(
                dim.width/2-connectionFrame.getSize().width,
                dim.height/2-connectionFrame.getSize().height/2
        );
    }

    private static class ConnectionFrame extends JFrame {

        private final JButton jButton;
        private final JLabel jLabel;
        private final DelayGraphPanel delayGraphPanel;

        private ConnectionFrame(final String title) {
            super(title);

            // construct components
            final JPanel jPanel = new JPanel();
            jButton = new JButton("Start");
            jLabel = new JLabel("Press start when server is ready", SwingConstants.CENTER);
            delayGraphPanel = new DelayGraphPanel();

            // adjust size and set layout
            jPanel.setPreferredSize(new Dimension(220, 360));
            jPanel.setLayout(null);

            // add components
            jPanel.add(jButton);
            jPanel.add(jLabel);
            jPanel.add(delayGraphPanel);

            // set component bounds (only needed by Absolute Positioning)
            jButton.setBounds(55, 45, 100, 20);
            jLabel.setBounds(10, 10, 200, 25);
            delayGraphPanel.setBounds(10,80,200,260);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getContentPane().add(jPanel);
            pack();
        }
    }

    private static class StartButtonListener implements ActionListener {

        private boolean started;
        long millis = 0;

        private InetAddress address;
        private DatagramSocket socket;

        private final ControllerService service;
        private final ConnectionFrame frame;
        private final String ip;
        private final int port;

        private StartButtonListener(final ControllerService service, final ConnectionFrame frame,
                                    final String ip, final int port) {
            this.service = service;
            this.frame = frame;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void actionPerformed(final ActionEvent actionEvent) {
            if (started) {
                started = false;
                service.finish();
                GeneralUtils.sleep(400); // Let the threads interrupt correctly

                try {
                    // Send "exit" packet
                    System.out.println("Sending exit command");
                    final byte[] exit = new byte[8];
                    exit[7] = UdpUtils.EXIT;
                    UdpUtils.sendPacket(socket, address, port, exit);

                    // Wait for server response
                    socket.setSoTimeout(CONNECTION_TIMEOUT);
                    final byte response = UdpUtils.receiveSingleByte(socket);
                    socket.setSoTimeout(0);

                    // Check that server response is valid
                    assert response == UdpUtils.EXIT_ACK;
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final IOException e) {
                    System.out.println("Couldn't onFinish connection. Closing anyways");
                    e.printStackTrace();
                }

                socket.close();
                frame.setVisible(false);
                showInitialFrame();
            } else {
                try {
                    // Datagram sockets don't have IP and port, they belong to individual packets
                    socket = new DatagramSocket();
                    // Get InetAddress from provided name
                    address = InetAddress.getByName(ip);

                    // Send "start" command
                    UdpUtils.sendSingleByte(socket, address, port, UdpUtils.START);

                    // Wait for server response
                    socket.setSoTimeout(CONNECTION_TIMEOUT);
                    final byte response = UdpUtils.receiveSingleByte(socket);
                    socket.setSoTimeout(0);

                    // Check that server response is valid
                    assert response == UdpUtils.START_ACK;
                    frame.jButton.setText("Exit");
                    started = true;

                    final Thread controller = new Thread(() -> {

                        int counter = 0;

                        service.start();
                        byte[] packet;
                        while (started) {
                            try {
                                packet = service.getControllerStatus();
                                if (packet.length == 0) {
                                    frame.jButton.doClick(); // will send exit command
                                    break;
                                } else {
                                    // Send ping every 50 packets
                                    if (counter >= 50) {
                                        counter = 0;
                                        packet[7] = UdpUtils.PING;
                                        millis = System.currentTimeMillis();
                                    } else {
                                        packet[7] = UdpUtils.NO_ACTION;
                                    }

                                    //System.out.println("Sending " + GeneralUtils.byteArrayToString(packet));
                                    UdpUtils.sendPacket(socket, address, port, packet);
                                    if (millis == 0) {
                                        counter++;
                                    }
                                    GeneralUtils.sleep(PERIOD_MS);
                                }
                            } catch (final IOException e) {
                                System.out.println("Error while sending controller state");
                                e.printStackTrace();
                                // TODO: socket.close();
                                // frame.setVisible(false);
                                // showInitialFrame();
                            }
                        }
                    });
                    controller.start();

                    final Thread frames = new Thread(() -> {
                        GeneralUtils.sleep(100);

                        final byte[] inputFrame = new byte[8];
                        while (started) {
                            try {
                                UdpUtils.receivePacket(socket, inputFrame);
                                // System.out.println("From server: " + ByteBuffer.wrap(inputFrame).getInt());
                                if (inputFrame[7] == UdpUtils.PING_REPLY) {
                                    final int delay = (int) (System.currentTimeMillis()-millis);
                                    frame.jLabel.setText("RTT: " + delay + " ms");
                                    frame.delayGraphPanel.addDelay(delay);
                                    millis = 0;
                                } /*else {
                                        ByteArrayInputStream bais = new ByteArrayInputStream(inputFrame);
                                        BufferedImage image = ImageIO.read(bais);
                                        if (image != null) {
                                            jImage.setIcon(new ImageIcon(image));
                                            frame.repaint();
                                        }
                                    }*/
                            } catch (final IOException e) {
                                System.out.println("Error while receiving frames");
                                e.printStackTrace();
                                // TODO: socket.close();
                                // frame.setVisible(false);
                                // showInitialFrame();
                            }
                        }
                    });
                    frames.start();
                } catch (final IOException e) {
                    System.out.println("Couldn't start connection with server");
                    JOptionPane.showMessageDialog(frame, e);
                    e.printStackTrace();
                    frame.setVisible(false);
                    showInitialFrame();
                }
            }
        }
    }
}
