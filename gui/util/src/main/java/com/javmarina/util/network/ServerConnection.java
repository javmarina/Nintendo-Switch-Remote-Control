package com.javmarina.util.network;

import java.io.IOException;


/**
 * Basic connection in which the device acts as a server. The server waits for clients to request a connection.
 */
public abstract class ServerConnection extends BaseConnection {

    /**
     * Wait for client connection requests.
     * @throws IOException if an error occurred.
     */
    public abstract void acceptConnection() throws IOException;
}
