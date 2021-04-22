package com.javmarina.client;

import com.javmarina.client.services.ControllerService;
import com.javmarina.client.services.DefaultJamepadService;
import com.javmarina.client.services.KeyboardService;
import com.javmarina.client.services.bot.DiscordService;
import com.javmarina.util.GeneralUtils;
import com.javmarina.util.network.ClientConnection;
import com.javmarina.util.network.protocol.ClientProtocol;
import com.javmarina.util.network.BaseConnection;
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
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.prefs.Preferences;


public final class Client {

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
        jPort.setText(prefs.get(KEY_PORT, String.valueOf(BaseConnection.DEFAULT_PORT)));

        final ArrayList<ControllerService> services = getAvailableServices();

        final int totalSize = services.size();
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

            final ClientConnection client;
            try {
                client = BaseConnection.newClientConnection(ip, Integer.parseInt(port));
            } catch (final SocketException e) {
                JOptionPane.showMessageDialog(frame, "Couldn't open socket. Select another port");
                return;
            } catch (final UnknownHostException e) {
                JOptionPane.showMessageDialog(frame, "Invalid IP address");
                return;
            }

            // Save preferences
            prefs.put(KEY_ADDRESS, ip);
            prefs.put(KEY_PORT, port);

            final ControllerService service = (ControllerService) jComboBox.getSelectedItem();
            showConnectionFrame(service, client);
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

    private static void showConnectionFrame(final ControllerService service, final ClientConnection client) {
        final ConnectionFrame connectionFrame =
                new ConnectionFrame("Client (server " + client.getServerDescription() + ')', service, client);
        connectionFrame.setResizable(false);
        connectionFrame.setVisible(true);

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

        private ConnectionFrame(final String title, final ControllerService service, final ClientConnection client) {
            super(title);

            // construct components
            final JPanel jPanel = new JPanel();
            jButton = new JButton("Start");
            jLabel = new JLabel("Press start when server is ready", SwingConstants.CENTER);
            delayGraphPanel = new DelayGraphPanel();

            // add action listener to button
            jButton.addActionListener(new ConnectionFrame.ButtonListener(service, this, client));

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

            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            getContentPane().add(jPanel);
            pack();
        }

        private static class ButtonListener implements ActionListener {

            private boolean started;

            private final ConnectionFrame frame;
            private final ClientProtocol protocol;

            private ButtonListener(final ControllerService service, final ConnectionFrame frame,
                                   final ClientConnection client) {
                this.frame = frame;
                this.protocol = new ClientProtocol(
                        client,
                        service::getControllerStatus,
                        new ClientProtocol.Callback() {
                            @Override
                            public void onRttReplyReceived(final int milliseconds) {
                                frame.jLabel.setText(String.format("RTT: %d ms", milliseconds));
                                frame.delayGraphPanel.addDelay(milliseconds);
                            }

                            @Override
                            public void onFrameReceived() {}

                            @Override
                            public void onSessionStarted() {
                                started = true;
                                service.start();
                                frame.jButton.setText("Exit");
                            }

                            @Override
                            public void onSessionStopped() {
                                service.finish();
                                frame.setVisible(false);
                                showInitialFrame();
                            }
                        });
            }

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (started) {
                    // "Exit" button clicked
                    started = false;
                    System.out.println("Stopping session");
                    protocol.stopSession(); // async method, onSessionStopped() will be called if successful
                } else {
                    // "Start" button clicked
                    try {
                        protocol.startSession(); // onSessionStarted() will be called if successful
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
}
