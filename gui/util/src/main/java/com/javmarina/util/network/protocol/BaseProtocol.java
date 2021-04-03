package com.javmarina.util.network.protocol;

import com.javmarina.util.Crc;
import com.javmarina.util.network.BaseConnection;

import java.io.IOException;


/**
 * This class implements the logic for reading and writing {@link Command}'s over a connection (wire format).
 * Read and write operations are synchronized. This class also provides the definition of available command IDs.
 * @param <T> type of connection to use depending on the role of the device
 *           ({@link com.javmarina.util.network.ClientConnection} or
 *           {@link com.javmarina.util.network.ServerConnection})
 */
public class BaseProtocol<T extends BaseConnection> {

    /**
     * Client to server. Petition to start a connection. No payload.
     */
    protected static final byte START = 0x22;

    /**
     * Server to client. {@link BaseProtocol#START} petition accepted, connection has started. No payload.
     */
    protected static final byte START_ACK = 0x33;

    /**
     * Client to server. The command payload contains a {@link com.javmarina.util.Packet}.
     */
    protected static final byte PACKET = 0x00;

    /**
     * Server to client. The command payload contains a frame.
     */
    protected static final byte FRAME = 0x01;

    /**
     * Client to server. Ping request. Payload contains the send timestamp as Epoch milliseconds
     * (see {@link System#currentTimeMillis()}).
     */
    protected static final byte PING = 0x45;

    /**
     * Server to client. Replies to {@link BaseProtocol#PING}. Payload must be the same.
     */
    protected static final byte PING_REPLY = 0x55;

    /**
     * Client to server. Petition to finish a connection. No payload.
     */
    protected static final byte EXIT = (byte) 0xFF;

    /**
     * Server to client. {@link BaseProtocol#EXIT} accepted, connection has ended. No payload.
     */
    protected static final byte EXIT_ACK = 0x11;

    private final T remoteConnection;
    private final Object readLock = new Object();
    private final Object writeLock = new Object();

    protected BaseProtocol(final T remoteConnection) {
        this.remoteConnection = remoteConnection;
    }

    /**
     * Receive a command from the remote device. Blocks until a command is received. This method is thread-safe.
     * @return the received command.
     * @throws IOException if an error occurred.
     */
    protected final Command receiveCommand() throws IOException {
        final byte[] response;
        synchronized (readLock) {
            response = remoteConnection.readBytes(258);
        }
        final byte length = response[0];
        // length comes in binary format. Use inverted two's complement
        final int lengthInt = length < 0 ? length + 256 : length;
        final byte id = response[1];
        final byte[] payload = new byte[lengthInt];
        System.arraycopy(response, 2, payload, 0, lengthInt);
        final byte crc = response[2+lengthInt];
        assert crc == Crc.fromBytes(payload);
        return new Command(id, payload);
    }

    /**
     * Send a command to the remote device. This method is thread-safe.
     * @param command the command to send.
     * @throws IOException if an error occurred.
     */
    protected final void sendCommand(final Command command) throws IOException {
        final byte[] payload = command.getPayload();
        final int length = payload.length;
        if (length > 255) {
            throw new IllegalArgumentException("Command must not be longer than 255 bytes. Actual size: " + length);
        }
        final byte[] buffer = new byte[2+length+1];
        buffer[0] = (byte) length;
        buffer[1] = command.getId();
        System.arraycopy(payload, 0, buffer, 2, length);
        buffer[buffer.length-1] = Crc.fromBytes(payload);

        synchronized (writeLock) {
            remoteConnection.writeBytes(buffer);
        }
    }
}
