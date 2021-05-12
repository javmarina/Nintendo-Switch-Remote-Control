package com.javmarina.signaling;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.annotation.WebServlet;

import java.util.logging.Logger;


/*
 * Server-side WebSocket upgraded on /signaling servlet.
 */
@WebServlet(
        name = "Signaling WebSocket Servlet",
        urlPatterns = {"/signaling"})
public final class SignalingServlet extends WebSocketServlet implements WebSocketCreator {

    private final Logger logger = Logger.getLogger(SignalingServlet.class.getName());

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator(this);
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
                                  ServletUpgradeResponse servletUpgradeResponse) {
        logger.fine("createWebSocket()");
        return new ServerSocket();
    }
}
