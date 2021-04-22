package com.javmarina.util.network.protocol;

import com.javmarina.util.Packet;
import com.javmarina.util.StoppableLoop;
import com.javmarina.util.network.ServerConnection;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Application-level server protocol. Periodically sends frames to the client and reacts to the packets received
 * from it accordingly. The IN and OUT endpoints run in parallel, communication is bidirectional.
 */
public final class ServerProtocol extends BaseProtocol<ServerConnection> {

    private final ServerConnection server;
    private final ServerProtocol.Callback callback;

    private final ServerIn serverInRunnable;
    private final ServerOut serverOutRunnable;
    private final Thread threadIn;
    private final Thread threadOut;

    public ServerProtocol(final ServerConnection server, final ServerProtocol.Callback callback) {
        super(server);
        this.server = server;
        this.callback = callback;

        this.serverInRunnable = new ServerIn();
        this.threadIn = new Thread(this.serverInRunnable);

        this.serverOutRunnable = new ServerOut();
        this.threadOut = new Thread(this.serverOutRunnable);
    }

    /**
     * Asynchronously wait for client connection.
     */
    public void waitForClientAsync() {
        new Thread(() -> {
            try {
                server.acceptConnection(); // Blocking

                final Command command = receiveCommand();
                assert command.getId() == BaseProtocol.START;
                sendStartAck();

                callback.onSessionStarted();
                threadIn.start();
                threadOut.start();
            } catch (final IOException e) {
                callback.onError(e);
            }
        }).start();
    }

    private void stopSession() {
        serverInRunnable.stop(() -> serverOutRunnable.stop(() -> {
            try {
                System.out.println("Sending EXIT_ACK");
                sendExitAck();

                server.close();
                callback.onSessionStopped();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private void sendStartAck() throws IOException {
        sendCommand(new Command(BaseProtocol.START_ACK));
    }

    private void sendFrame(final byte[] frame) throws IOException {
        sendCommand(new Command(BaseProtocol.FRAME, frame));
    }

    private void sendPingReply(final byte[] payload) throws IOException {
        sendCommand(new Command(BaseProtocol.PING_REPLY, payload));
    }

    private void sendExitAck() throws IOException {
        sendCommand(new Command(BaseProtocol.EXIT_ACK));
    }

    private final class ServerIn extends StoppableLoop {

        @Override
        public void loop() {
            try {
                final Command command = receiveCommand();
                switch (command.getId()) {
                    case BaseProtocol.PACKET:
                        callback.onPacketReceived(new Packet(command.getPayload()));
                        break;
                    case BaseProtocol.PING:
                        System.out.println("Sending ping_reply");
                        sendPingReply(command.getPayload());
                        break;
                    case BaseProtocol.EXIT:
                        stopSession();
                        break;
                }
            } catch (final IOException e) {
                // Not a fatal error, don't call onError()
                e.printStackTrace();
            }
        }
    }

    private final class ServerOut extends StoppableLoop {

        private static final int FPS = 60;
        private static final int PERIOD_MS = 1000/FPS;

        private int i = 0;
        private long millis = 0;
        private final ByteBuffer buffer = ByteBuffer.allocate(8);

        @Override
        public void loop() {
            try {
                if (System.currentTimeMillis() - millis > PERIOD_MS) {
                    sendFrame(buffer.putInt(0, i).array());
                    millis = System.currentTimeMillis();
                    i++;
                }
            } catch (final IOException e) {
                // Not a fatal error, don't call onError()
                e.printStackTrace();
            }
        }
    }

    public interface Callback {
        /**
         * New packet received from client.
         * @param packet the received packet.
         */
        void onPacketReceived(final Packet packet);

        /**
         * Client requested a connection and it has been established successfully.
         */
        void onSessionStarted();

        /**
         * Session has been stopped correctly.
         */
        void onSessionStopped();

        /**
         * An error occurred.
         * @param e the error.
         */
        void onError(final Exception e);
    }
}
