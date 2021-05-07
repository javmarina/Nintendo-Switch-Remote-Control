package com.javmarina.webrtc.signaling;

import com.javmarina.util.Crc;
import com.javmarina.util.Command;
import com.javmarina.webrtc.JsonCodec;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class BaseSignaling {

    /**
     * Default port for both client and server. User is able to change it.
     */
    public static final int DEFAULT_PORT = 32800;

    public static final byte OFFER_COMMAND = 0x013;
    public static final byte ANSWER_COMMAND = 0x028;
    public static final byte NEW_ICE_CANDIDATE_COMMAND = 0x03F;

    private final Object readLock = new Object();
    private final Object writeLock = new Object();

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    void setSocket(final Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public void sendOffer(final RTCSessionDescription description) throws IOException {
        sendCommand(new Command(
                OFFER_COMMAND,
                JsonCodec.encode(description).getBytes(StandardCharsets.UTF_8)
        ));
    }

    public void sendAnswer(final RTCSessionDescription description) throws IOException {
        sendCommand(new Command(
                ANSWER_COMMAND,
                JsonCodec.encode(description).getBytes(StandardCharsets.UTF_8)
        ));
    }

    public void sendIceCandidate(final RTCIceCandidate candidate) throws IOException {
        sendCommand(new Command(
                NEW_ICE_CANDIDATE_COMMAND,
                JsonCodec.encode(candidate).getBytes(StandardCharsets.UTF_8)
        ));
    }

    /**
     * Receive a command from the remote device. Blocks until a command is received. This method is thread-safe.
     * @return the received command.
     * @throws IOException if an error occurred.
     */
    public final Command receiveCommand() throws IOException {
        if (in.available() == 0) {
            return null;
        }
        synchronized (readLock) {
            final byte lengthSize = in.readByte();
            final byte[] lengthBuffer = in.readNBytes(lengthSize);
            int length = 0;
            for (int i = 0; i < lengthSize; i++) {
                length <<= 8;
                final int val = lengthBuffer[i];
                length |= (val < 0 ? val + 256 : val);
            }

            final byte id = in.readByte();
            final byte[] payload = in.readNBytes(length);
            final byte crc = in.readByte();
            if (crc != Crc.fromBytes(payload)) {
                throw new RuntimeException("CRC not valid");
            }
            return new Command(id, payload);
        }
    }

    /**
     * Send a command to the remote device. This method is thread-safe.
     * @param command the command to send.
     * @throws IOException if an error occurred.
     */
    public final void sendCommand(final Command command) throws IOException {
        final byte[] payload = command.getPayload();
        final int length = payload.length;

        int mask = 0xFF;
        int lengthSize;
        for (lengthSize = 1; lengthSize <= 4; lengthSize++) {
            if ((length & ~mask) == 0) {
                break;
            }
            mask = (mask << 8) | 0xFF;
        }

        final int bufferSize = 1+lengthSize+1+length+1;

        final byte[] buffer = new byte[bufferSize];
        buffer[0] = (byte) lengthSize;
        int val = length;
        for (int i = 0; i < lengthSize; i++) {
            buffer[lengthSize-i] = (byte) (val & 0xFF);
            val >>>= 8;
        }
        buffer[1+lengthSize] = command.getId();
        System.arraycopy(payload, 0, buffer, 2+lengthSize, length);
        buffer[1+lengthSize+1+length] = Crc.fromBytes(payload);
        synchronized (writeLock) {
            out.write(buffer, 0, buffer.length);
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    public static void validatePort(final int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port value: " + port);
        }
    }
}
