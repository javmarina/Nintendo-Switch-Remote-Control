package com.javmarina.util.network.tcp;

import com.javmarina.util.network.BaseConnection;
import com.javmarina.util.network.ClientConnection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


public final class TcpClient extends ClientConnection {

    private final TcpDelegate delegate;

    private final InetAddress address;
    private final int port;

    public TcpClient(final String ip, final int port) throws UnknownHostException {
        this(InetAddress.getByName(ip), port);
    }

    public TcpClient(final InetAddress address, final int port) {
        BaseConnection.validatePort(port);
        this.address = address;
        this.port = port;
        this.delegate = new TcpDelegate();
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
        return address.getHostAddress() + ":" + port;
    }

    @Override
    public void requestConnection() throws IOException {
        final Socket socket = new Socket(address, port);
        delegate.setSocket(socket);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
