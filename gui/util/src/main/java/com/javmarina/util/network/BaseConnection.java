package com.javmarina.util.network;

import com.javmarina.util.network.udp.UdpClient;
import com.javmarina.util.network.udp.UdpServer;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;


/**
 * Abstract class that represents a connection with another device. One device can send bytes to the other device
 * and receive bytes from it, as well as configuring the read/write timeout.
 */
public abstract class BaseConnection implements Closeable {

    /**
     * Default port for both client and server. User is able to change it.
     */
    public static final int DEFAULT_PORT = 32800;

    public abstract byte[] readBytes(final int bytesToRead) throws IOException;
    public abstract void writeBytes(final byte[] data, int length) throws IOException;
    public abstract void setSoTimeout(final int milliseconds) throws SocketException;

    public void writeBytes(final byte... data) throws IOException {
        writeBytes(data, data.length);
    }

    public static void validatePort(final int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port value: " + port);
        }
    }

    /*
     * Factory methods
     */

    public static ClientConnection newClientConnection(final String ip, final int port)
            throws SocketException, UnknownHostException {
        return new UdpClient(ip, port);
    }

    public static ServerConnection newServerConnection(final int serverPort) throws SocketException {
        return new UdpServer(serverPort);
    }
}
