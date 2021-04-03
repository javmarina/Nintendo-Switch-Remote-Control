package com.javmarina.client.services;

import com.javmarina.util.Packet;
import org.jetbrains.annotations.Nullable;


/**
 * This class represents any entity that can generate a controller input. It doesn't need to be a real controller, as it
 * can also be a keyboard, a bot, a TAS tool...<br>
 * Subclasses will be asked for a controller report periodically by calling {@link ControllerService#getPacket()}. They
 * must return a report with the format specified in {@link com.javmarina.util.Packet}. The update rate is not
 * specified, so implementations that rely on emulated inputs (like reading the commands from a file) should manage
 * timing accordingly.<br>
 * This class is designed as a Finite State Machine with three states: READY (object created but input capture not
 * started), RUNNING (capturing input) and FINISHED (input is no longer needed). A running instance cannot go back to
 * STARTED, and a finished instance can't run again.
 */
public abstract class ControllerService {

    private enum Status {
        READY,
        RUNNING,
        FINISHED
    }

    private Status status = Status.READY;

    /**
     * Subclasses must override this method to provide a valid packet.
     * @return an packet with the controller input.
     */
    protected abstract Packet getPacket();

    /**
     * Callback called when the status changes to FINISHED. Subclasses can perform the finishing steps needed.
     */
    protected void onFinish() {}

    /**
     * Callback called when the status changes to RUNNING. Subclasses can perform initialization actions.
     */
    protected void onStart() {}

    /**
     * Get current controller input packet
     * @return the packet with controller input, or null if the service is finished or not started.
     */
    @Nullable
    public final Packet getControllerStatus() {
        if (status == Status.RUNNING) {
            return getPacket();
        } else {
            return null;
        }
    }

    /**
     * Finish this service. Call this method when reports are no longer needed. Subsequent calls to
     * {@link ControllerService#getControllerStatus()} will return an empty packet.
     */
    public void finish() {
        if (status == Status.RUNNING) {
            status = Status.FINISHED;
            onFinish();
        }
    }

    /**
     * Start the service. Valid packets will be returned by {@link ControllerService#getControllerStatus()}.
     */
    public void start() {
        if (status == Status.READY) {
            status = Status.RUNNING;
            onStart();
        }
    }

    /**
     * All services must have a name
     * @return the name of the service, which will appear on the UI
     */
    @Override
    public abstract String toString();
}
