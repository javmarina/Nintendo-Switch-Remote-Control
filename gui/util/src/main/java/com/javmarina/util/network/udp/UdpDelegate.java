package com.javmarina.util.network.udp;

import com.javmarina.util.network.BaseConnection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


final class UdpDelegate {

    private InetAddress address;
    private int port;
    private final DatagramSocket datagramSocket;

    UdpDelegate(final String ip, final int port) throws UnknownHostException, SocketException {
        this(InetAddress.getByName(ip), port);
    }

    UdpDelegate(final InetAddress address, final int port) throws SocketException {
        BaseConnection.validatePort(port);
        this.address = address;
        this.port = port;

        this.datagramSocket = new DatagramSocket();
    }

    UdpDelegate(final int serverPort) throws SocketException {
        BaseConnection.validatePort(serverPort);
        this.datagramSocket = new DatagramSocket(serverPort);
    }

    void setRemoteHost(final InetAddress address, final int port) {
        this.address = address;
        this.port = port;
    }

    byte[] readBytes(final int bytesToRead) throws IOException {
        if (datagramSocket == null) {
            throw new IOException("Connection not established");
        }
        final byte[] data = new byte[bytesToRead];
        final DatagramPacket udpPacket = new DatagramPacket(data, data.length);
        datagramSocket.receive(udpPacket);
        return data;
    }

    void writeBytes(final byte[] data, final int length) throws IOException {
        if (datagramSocket == null) {
            throw new IOException("Connection not established");
        }
        final DatagramPacket udpPacket = new DatagramPacket(data, length, address, port);
        datagramSocket.send(udpPacket);
    }

    void setSoTimeout(final int milliseconds) throws SocketException {
        if (datagramSocket == null) {
            throw new SocketException("Connection not established");
        }
        datagramSocket.setSoTimeout(milliseconds);
    }

    void receive(final DatagramPacket p) throws IOException {
        datagramSocket.receive(p);
    }

    @NotNull
    public String getRemoteDescription() {
        return address.getHostAddress() + ":" + port;
    }

    void close() throws IOException {
        if (datagramSocket == null) {
            throw new IOException("Connection not established");
        }
        datagramSocket.close();
    }
}
