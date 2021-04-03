package com.javmarina.util.network.udp;

import com.javmarina.util.network.BaseConnection;
import com.javmarina.util.network.ServerConnection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;


public final class UdpServer extends ServerConnection {

    private final UdpDelegate delegate;

    public UdpServer(final int serverPort) throws SocketException {
        BaseConnection.validatePort(serverPort);
        this.delegate = new UdpDelegate(serverPort);
    }

    @Override
    public void acceptConnection() throws IOException {
        final byte[] buf = new byte[0];
        final DatagramPacket packet = new DatagramPacket(buf, buf.length);
        delegate.receive(packet);

        delegate.setRemoteHost(packet.getAddress(), packet.getPort());
    }

    @Override
    public byte[] readBytes(final int bytesToRead) throws IOException {
        return delegate.readBytes(bytesToRead);
    }

    @Override
    public void writeBytes(final byte[] data, final int length) throws IOException {
        delegate.writeBytes(data, length);
    }

    @Override
    public void setSoTimeout(final int milliseconds) throws SocketException {
        delegate.setSoTimeout(milliseconds);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
