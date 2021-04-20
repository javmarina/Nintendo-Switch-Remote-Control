package com.javmarina.webrtc.signaling;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerSideSignaling extends BaseSignaling {

    private final ServerSocket serverSocket;

    public ServerSideSignaling(final int serverPort) throws IOException {
        BaseSignaling.validatePort(serverPort);
        this.serverSocket = new ServerSocket(serverPort);
    }

    public void acceptConnection() throws IOException {
        final Socket socket = serverSocket.accept();
        setSocket(socket);
    }

    @Override
    public void close() throws IOException {
        super.close();
        serverSocket.close();
    }
}
