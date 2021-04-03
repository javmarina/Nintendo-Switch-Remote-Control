package com.javmarina.util.network.tcp;

import com.javmarina.util.network.BaseConnection;
import com.javmarina.util.network.ServerConnection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public final class TcpServer extends ServerConnection {

    private final TcpDelegate delegate;

    @NotNull
    private final ServerSocket serverSocket;

    public TcpServer(final int serverPort) throws IOException {
        BaseConnection.validatePort(serverPort);
        this.serverSocket = new ServerSocket(serverPort);
        delegate = new TcpDelegate();
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
    public void acceptConnection() throws IOException {
        final Socket socket = serverSocket.accept();
        delegate.setSocket(socket);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        serverSocket.close();
    }
}
