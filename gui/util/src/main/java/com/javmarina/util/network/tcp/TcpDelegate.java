package com.javmarina.util.network.tcp;

import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;


final class TcpDelegate {

    @Nullable
    private Socket socket;
    @Nullable
    private DataInputStream in;
    @Nullable
    private DataOutputStream out;

    TcpDelegate() {
    }

    void setSocket(final Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    byte[] readBytes(final int bytesToRead) throws IOException {
        if (in == null) {
            throw new IOException("Connection not established");
        }
        final byte[] data = new byte[bytesToRead];
        in.read(data);
        return data;
    }

    void writeBytes(final byte[] data, final int length) throws IOException {
        if (out == null) {
            throw new IOException("Connection not established");
        }
        out.write(data, 0, length);
    }

    void setSoTimeout(final int milliseconds) throws SocketException {
        if (socket == null) {
            throw new SocketException("Connection not established");
        }
        socket.setSoTimeout(milliseconds);
    }

    void close() throws IOException {
        if (socket == null) {
            throw new IOException("Connection not established");
        }
        socket.close();
    }
}
