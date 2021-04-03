package com.javmarina.util.network.protocol;

import com.javmarina.util.GeneralUtils;
import com.javmarina.util.Packet;
import com.javmarina.util.StoppableLoop;
import com.javmarina.util.network.ClientConnection;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


/**
 * Application-level client protocol. Periodically sends packets to the server and reacts to the data received
 * from it accordingly. The IN and OUT endpoints run in parallel, communication is bidirectional.
 */
public final class ClientProtocol extends BaseProtocol<ClientConnection> {

    // Timeout for establishing or ending a connection with server
    private static final int CONNECTION_TIMEOUT = 2000; // 2 seconds

    private final ClientConnection client;
    private final PacketProvider packetProvider;
    private final Callback callback;

    private final ClientIn clientInRunnable;
    private final ClientOut clientOutRunnable;
    private final Thread threadIn;
    private final Thread threadOut;

    public ClientProtocol(final ClientConnection client, final PacketProvider packetProvider, final Callback callback) {
        super(client);
        this.client = client;
        this.packetProvider = packetProvider;
        this.callback = callback;

        this.clientInRunnable = new ClientIn();
        this.threadIn = new Thread(this.clientInRunnable);

        this.clientOutRunnable = new ClientOut();
        this.threadOut = new Thread(this.clientOutRunnable);
    }

    /**
     * Start a new session (discover server and request a connection)
     * @throws IOException if an error occurred.
     */
    public void startSession() throws IOException {
        // Send "start" command
        client.requestConnection();
        sendStart();

        // Wait for server response
        client.setSoTimeout(CONNECTION_TIMEOUT);
        final Command command = receiveCommand();
        client.setSoTimeout(0);

        // Check that server response is valid
        assert command.getId() == BaseProtocol.START_ACK;

        callback.onSessionStarted();
        threadIn.start();
        threadOut.start();
    }

    /**
     * End running session (send {@link BaseProtocol#EXIT} and close connection)
     */
    public void stopSession() {
        clientOutRunnable.stop(() -> clientInRunnable.stop(() -> {
            try {
                // Send "exit" packet
                System.out.println("Sending EXIT to server");
                sendExit();

                // Wait for server response
                client.setSoTimeout(CONNECTION_TIMEOUT);
                final Command command = receiveCommand();
                client.setSoTimeout(0);

                // Check that server response is valid
                assert command.getId() == BaseProtocol.EXIT_ACK;

                client.close();
                callback.onSessionStopped();
            } catch (final IOException e) {
                System.out.println("Couldn't stop session");
                e.printStackTrace();
            }
        }));
    }

    private void sendStart() throws IOException {
        sendCommand(new Command(BaseProtocol.START));
    }

    private void sendPacket(final Packet packet) throws IOException {
        System.out.println("Sending " + GeneralUtils.byteArrayToString(packet.getBuffer()));
        sendCommand(new Command(BaseProtocol.PACKET, packet.getBuffer()));
    }

    private void sendPing() throws IOException {
        sendCommand(new Command(BaseProtocol.PING, longToBytes(System.currentTimeMillis())));
    }

    private void sendExit() throws IOException {
        sendCommand(new Command(BaseProtocol.EXIT));
    }

    private static byte[] longToBytes(final long l) {
        long val = l;
        final byte[] result = new byte[Long.BYTES];
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            result[i] = (byte)(val & 0xFF);
            val >>= Byte.SIZE;
        }
        return result;
    }

    private static long bytesToLong(final byte[] b) {
        long result = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            result <<= Byte.SIZE;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    private final class ClientIn extends StoppableLoop {

        @Override
        public void loop() {
            try {
                final Command command = receiveCommand();
                switch (command.getId()) {
                    case BaseProtocol.PING_REPLY:
                        callback.onRttReplyReceived(
                                (int) (System.currentTimeMillis() - bytesToLong(command.getPayload()))
                        );
                        break;
                    case BaseProtocol.FRAME:
                        final String message = GeneralUtils.byteArrayToString(command.getPayload());
                        System.out.println("Received frame: " + message);
                        break;
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final class ClientOut extends StoppableLoop {

        private static final int PERIOD_MS = 5;
        private static final int RTT_PACKET_COUNTER = 50;

        private long millis = 0;
        private int packetCounter = 0;

        @Override
        public void loop() {
            try {
                if (System.currentTimeMillis() - millis > PERIOD_MS) {
                    final Packet packet = packetProvider.getPacket();
                    if (packet == null) {
                        stopSession();
                        return;
                    } else {
                        sendPacket(packet);
                        millis = System.currentTimeMillis();
                        packetCounter++;
                    }
                }

                if (packetCounter > RTT_PACKET_COUNTER) {
                    packetCounter = 0;
                    sendPing();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface PacketProvider {

        /**
         * Called when a new {@link Packet} is to be sent to the sever.
         * @return the packet to send, or {@code null} if no more packets can be sent
         * and the connection should be closed.
         */
        @Nullable
        Packet getPacket();
    }

    public interface Callback {
        /**
         * New RTT value computed.
         * @param milliseconds new RTT in milliseconds.
         */
        void onRttReplyReceived(final int milliseconds);

        /**
         * New frame received from server.
         */
        void onFrameReceived();

        /**
         * Connection with server has been established.
         */
        void onSessionStarted();

        /**
         * Connection with server successfully closed.
         */
        void onSessionStopped();
    }
}
