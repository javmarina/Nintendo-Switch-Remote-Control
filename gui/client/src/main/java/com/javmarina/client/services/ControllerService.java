package com.javmarina.client.services;


/**
 * This class represents any entity that can generate a controller input. It doesn't need to be a real controller, as it
 * can also be a keyboard, a bot, a TAS tool...<br>
 * Subclasses will be asked for a controller report periodically by calling {@link ControllerService#getMessage()}. They
 * must return a report with the format specified in {@link com.javmarina.util.Controller}. The update rate is not
 * specified, so implementations that rely on emulated inputs (like reading the commands from a file) should manage
 * timing accordingly.<br>
 * This class is designed as a Finite State Machine with three states: READY (object created but input capture not
 * started), RUNNING (capturing input) and FINISHED (input is no longer needed). A running instance cannot go back to
 * STARTED, and a finished instance can't run again.
 */
public abstract class ControllerService {

    private final byte[] emptyMessage = new byte[0];

    private enum Status {
        READY,
        RUNNING,
        FINISHED
    }

    private Status status = Status.READY;

    /**
     * Subclasses must override this method to provide a valid packet (8-byte array). Format specified in
     * {@link com.javmarina.util.Controller}.
     * @return an 8-byte array with the controller input.
     */
    public abstract byte[] getMessage();

    /**
     * Callback called when the status changes to FINISHED. Subclasses can perform the finishing steps needed.
     */
    public void onFinish() {}

    /**
     * Callback called when the status changes to RUNNING. Subclasses can perform initialization actions.
     */
    public void onStart() {}

    /**
     * Get current controller input packet
     * @return the byte array (packet) with controller input, or an empty array if the service is finished or not started.
     */
    public byte[] getControllerStatus() {
        if (status == Status.RUNNING) {
            final byte[] message = getMessage();
            if (message[7] != 0x00) {
                new RuntimeException("Provided vendorspec must be 0x00, found " + message[7]).printStackTrace();
                message[7] = 0x00;
            }
            return message;
        } else {
            return emptyMessage;
        }
    }

    /**
     * Finish this service. Call this method when reports are no longer needed. Subsequent calls to
     * {@link ControllerService#getControllerStatus()} will return an empty array.
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
