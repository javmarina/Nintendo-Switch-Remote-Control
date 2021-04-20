package com.javmarina.webrtc.signaling;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class ClientSideSignaling extends BaseSignaling {

    private final InetAddress address;
    private final int port;

    public ClientSideSignaling(final String ip, final int port) throws UnknownHostException {
        this(InetAddress.getByName(ip), port);
    }

    public ClientSideSignaling(final InetAddress address, final int port) {
        BaseSignaling.validatePort(port);
        this.address = address;
        this.port = port;
    }

    public void requestConnection() throws IOException {
        final Socket socket = new Socket(address, port);
        setSocket(socket);
    }
}
