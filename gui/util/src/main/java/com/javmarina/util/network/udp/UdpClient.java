package com.javmarina.util.network.udp;

import com.javmarina.util.network.ClientConnection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class UdpClient extends ClientConnection {

    private final UdpDelegate delegate;

    public UdpClient(final String serverIp, final int serverPort) throws UnknownHostException, SocketException {
        this.delegate = new UdpDelegate(serverIp, serverPort);
    }

    public UdpClient(final InetAddress serverAddress, final int serverPort) throws SocketException {
        this.delegate = new UdpDelegate(serverAddress, serverPort);
    }

    @Override
    public void requestConnection() throws IOException {
        final byte[] data = new byte[0];
        writeBytes(data);
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

    @NotNull
    @Override
    public String getServerDescription() {
        return delegate.getRemoteDescription();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
