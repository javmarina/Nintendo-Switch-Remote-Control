package com.javmarina.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


/**
 * Class with utilities for sending and receiving data over UDP. Data can be either single bytes or 8-byte <b>packets</b>.
 * Single bytes are used for specific control action, such as starting or finishing a connection. 8-byte packets contain
 * controller input and a control byte, and are sent continuously.
 * <br> In both cases, an additional CRC byte is added, so in fact sent data can be either 2 or 9 bytes long. Send methods
 * take care of computing the CRC, while receive methods check if the CRC is valid and return the data without it. The
 * idea is that classes that use {@link UdpUtils} don't need to think about the CRC. This allows to modify the internal
 * implementation of the CRC computation without classes noticing.
 */
public final class UdpUtils {

    /**
     * Default UDP port for both client and server. User is able to change it.
     */
    public static final int DEFAULT_PORT = 32800;

    /**
     * Client to server byte. Petition to finish a connection. <b>Send within a packet</b>, because server expects 8 bytes
     * during connection. The rest of the packet can be empty.
     */
    public static final byte EXIT = (byte) 0xFF;

    /**
     * Server to client byte. {@link UdpUtils#EXIT} accepted, connection has ended. <b>Send as a single byte</b>.
     */
    public static final byte EXIT_ACK = 0x11;

    /**
     * Client to server byte. Petition to start a connection. <b>Send as a single byte</b>.
     */
    public static final byte START = 0x22;

    /**
     * Server to client byte. {@link UdpUtils#START} petition accepted, connection has started. <b>Send as a single byte</b>.
     */
    public static final byte START_ACK = 0x33;

    /**
     * Client to server. No action associated with the packet. <b>Send within a packet</b>.
     */
    public static final byte NO_ACTION = 0x00;

    /**
     * Client to server. In addition to the controller input, the packet includes a ping petition. <b>Send within a packet</b>.
     */
    public static final byte PING = 0x45;

    /**
     * Server to client. Reply to {@link UdpUtils#PING}. <b>Send within a packet</b>, because client expects 8 bytes
     * during connection. The rest of the packet can be empty.
     */
    public static final byte PING_REPLY = 0x55;

    private static final int PACKET_LENGTH = 8;
    private static final int BUFFER_CAPACITY = PACKET_LENGTH + 1;
    // Buffers used for packets (8+1 bytes)
    private static final byte[] outPacketBuff = new byte[BUFFER_CAPACITY];
    private static final byte[] inPacketBuff = new byte[BUFFER_CAPACITY];
    // Buffers used for single bytes (1+1 bytes)
    private static final byte[] outByteBuff = new byte[2];
    private static final byte[] inByteBuff = new byte[2];

    /**
     * Send a single byte over UDP.
     * @param socket UDP socket to use.
     * @param address IP address of the receiver.
     * @param port UDP port in the receiver.
     * @param b byte that is going to be sent.
     * @throws IOException if something went wrong.
     */
    public static void sendSingleByte(@NotNull final DatagramSocket socket, @NotNull final InetAddress address,
                                      final int port, final byte b) throws IOException {
        outByteBuff[0] = b;
        outByteBuff[1] = CrcUtils.crc(outByteBuff, 1);
        final DatagramPacket packet = new DatagramPacket(outByteBuff, outByteBuff.length, address, port);
        socket.send(packet);
    }

    /**
     * Send a packet (8 bytes) over UDP.
     * @param socket UDP socket to use.
     * @param address IP address of the receiver.
     * @param port UDP port in the receiver.
     * @param packet data that is going to be sent. Must have 8 bytes.
     * @throws IOException if something went wrong.
     */
    public static void sendPacket(@NotNull final DatagramSocket socket, @NotNull final InetAddress address,
                                  final int port, final byte[] packet) throws IOException {
        if (packet.length != PACKET_LENGTH) {
            throw new IllegalArgumentException("Provided packet has incorrect size (" + packet.length +
                    " bytes), must be " + PACKET_LENGTH);
        }
        System.arraycopy(packet, 0, outPacketBuff, 0, PACKET_LENGTH);
        outPacketBuff[PACKET_LENGTH] = CrcUtils.crc(packet);
        final DatagramPacket udpPacket = new DatagramPacket(outPacketBuff, outPacketBuff.length, address, port);
        socket.send(udpPacket);
    }

    /**
     * Receive a single byte over UDP.
     * @param socket UDP socket to use.
     * @return the received byte.
     * @throws IOException if something went wrong.
     */
    public static byte receiveSingleByte(@NotNull final DatagramSocket socket)
            throws IOException {
        final DatagramPacket packet = new DatagramPacket(inByteBuff, 2);
        socket.receive(packet);

        // Check CRC
        final byte expected = CrcUtils.crc(inByteBuff, 1);
        final byte found = inByteBuff[1];
        if (expected == found) {
            return inByteBuff[0];
        } else {
            throw new InvalidCrcException("Unmatched CRC. Expected " + expected + ", found " + found);
        }
    }

    /**
     * Receive a packet (8 bytes) over UDP.
     * @param socket UDP socket to use.
     * @param packet array that will be populated with the received data.
     * @throws IOException if something went wrong.
     */
    public static void receivePacket(@NotNull final DatagramSocket socket, final byte[] packet)
            throws IOException {
        if (packet.length != PACKET_LENGTH) {
            throw new IllegalArgumentException("Provided buffer has incorrect size (" + packet.length +
                    " bytes), must be " + PACKET_LENGTH);
        }
        final DatagramPacket udpPacket = new DatagramPacket(inPacketBuff, inPacketBuff.length);
        socket.receive(udpPacket);

        // Check CRC
        final byte expected = CrcUtils.crc(inPacketBuff, PACKET_LENGTH);
        final byte found = inPacketBuff[PACKET_LENGTH];
        if (expected == found) {
            System.arraycopy(inPacketBuff, 0, packet, 0, PACKET_LENGTH);
        } else {
            throw new InvalidCrcException("Unmatched CRC. Expected " + expected + ", found " + found);
        }
    }

    /**
     * CRC of the transferred data is not valid.
     */
    public static class InvalidCrcException extends IOException {

        InvalidCrcException(final String message) {
            super(message);
        }
    }
}
