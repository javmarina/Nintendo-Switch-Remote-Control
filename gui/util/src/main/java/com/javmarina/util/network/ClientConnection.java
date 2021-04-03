package com.javmarina.util.network;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;


/**
 * Basic connection in which a client (this device) connects to a server. The client must request a connection to
 * the server before sending and receiving data.
 */
public abstract class ClientConnection extends BaseConnection {

    /**
     * Request connection to the server.
     * @throws IOException if the request was refused or an error occurred.
     */
    public abstract void requestConnection() throws IOException;

    /**
     * Get a descriptive string about the server.
     * @return string with information about the sever. Can be it's IP address and port.
     */
    @NotNull
    public abstract String getServerDescription();
}
