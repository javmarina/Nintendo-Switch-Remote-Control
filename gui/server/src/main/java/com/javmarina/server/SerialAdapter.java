package com.javmarina.server;

import com.fazecast.jSerialComm.SerialPort;
import com.javmarina.util.Crc;
import com.javmarina.util.GeneralUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;
import com.javmarina.util.Packet;

import java.io.IOException;


@SuppressWarnings("NumericCastThatLosesPrecision")
public class SerialAdapter {

    private enum Status {
        OUT_OF_SYNC,
        SYNCED,
        SYNCING
    }

    private static final int WRITE_TIMEOUT = 0; // Blocking write
    // TODO: Change constant to Windows latency+2
    private static final int READ_TIMEOUT = 18; // Default buffer delay in Windows is 16ms

    // Commands to send to MCU
    //private static final byte COMMAND_NOP = 0x00;
    private static final byte COMMAND_SYNC_1 = 0x33;
    private static final byte COMMAND_SYNC_2 = (byte) 0xCC;
    private static final byte COMMAND_SYNC_START = (byte) 0xFF;

    // Responses from MCU
    //private static final byte RESP_USB_ACK = (byte) 0x90;
    private static final byte RESP_UPDATE_ACK = (byte) 0x91;
    private static final byte RESP_UPDATE_NACK = (byte) 0x92;
    private static final byte RESP_SYNC_START = (byte) 0xFF;
    private static final byte RESP_SYNC_1 = (byte) 0xCC;
    private static final byte RESP_SYNC_OK = 0x33;

    @Nullable
    private final SerialPort serialPort;
    private Status status = Status.OUT_OF_SYNC;
    private boolean isBaudrateInvalid = false;

    public SerialAdapter(@Nullable final SerialPort serialPort, final int baudrate) {
        if (serialPort != null) {
            serialPort.setNumDataBits(8);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING,
                    READ_TIMEOUT,
                    WRITE_TIMEOUT);
            if (SystemUtils.IS_OS_WINDOWS) {
                // Allow elevated privileges. If user is using an FTDI device,
                // we will be able to reduce latency timer
                serialPort.allowElevatedPermissionsRequest();
            }
            serialPort.openPort();
            if (!serialPort.setBaudRate(baudrate)) {
                this.isBaudrateInvalid = true;
            }
        }
        this.serialPort = serialPort;
    }

    public boolean isBaudrateInvalid() {
        return isBaudrateInvalid;
    }

    /*
    1. Send nine 0xFF bytes.
    2. Wait 1ms, then read back all the bytes that have been received. You can receive anywhere from 1 to 9 bytes, and the
    response will always end in 0xFF. Note that you may receive multiple 0xFF bytes. Alternatively, you can clear the
    receive buffer, then send another 0xFF byte and wait for a response back - when the AVR is not in "synchronized"
    state, it will always send a 0xFF byte to signal that it is ready to begin synchronization.
    3. Send a 0x33 byte and wait for the response (0xCC).
    4. Send a 0xCC byte and wait for the response (0x33).
    */
    @SuppressWarnings("ReuseOfLocalVariable")
    public void sync(final boolean forceSync) throws IOException {
        if (serialPort == null) {
            status = Status.SYNCED;
            return;
        }
        final long t1 = System.currentTimeMillis();
        if (status == Status.SYNCING) {
            // Another thread started syncing process and it hasn't finished yet
            return;
        }
        if (status == Status.SYNCED && !forceSync) {
            // Already synced and don't want to force sync
            return;
        }
        status = Status.SYNCING;
        errorRate = 0.0f;

        // Send 9x 0xFF's to fully flush out buffer on device
        // Device will send back 0xFF (RESP_SYNC_START) when it is ready to sync
        final byte b = COMMAND_SYNC_START;
        final byte[] bufferFlushBytes = {b,b,b,b,b,b,b,b,b};
        serialPort.writeBytes(bufferFlushBytes, bufferFlushBytes.length);
        System.out.println("Bytes written");

        long timestamp = System.currentTimeMillis();
        int available = 0;
        while (System.currentTimeMillis() - timestamp < READ_TIMEOUT) {
            final int now = serialPort.bytesAvailable();
            if (now > available) {
                available = now;
                timestamp = System.currentTimeMillis();
            }
        }
        if (available >= 1 && available <= 9) {
            // OK, read last received byte
            final byte[] rx = new byte[available];
            serialPort.readBytes(rx, available);
            System.out.println("Received " + available + " bytes: " + GeneralUtils.byteArrayToString(rx));
            if (rx[available-1] == RESP_SYNC_START) {
                // MCU ready to start synchronization
                System.out.println("RESP_SYNC_START received as last byte");
                sendByte(COMMAND_SYNC_1);
                System.out.println("Sending COMMAND_SYNC_1");
                byte response = readByte();
                System.out.println("Response: " + response);

                if (response == RESP_SYNC_1) {
                    // First step done
                    System.out.println("RESP_SYNC_1 received");
                    sendByte(COMMAND_SYNC_2);
                    System.out.println("Sending COMMAND_SYNC_2");
                    response = readByte();
                    System.out.println("Response: " + response);

                    if (response == RESP_SYNC_OK) {
                        // Synchronized!!
                        System.out.println("RESP_SYNC_OK received");
                        status = Status.SYNCED;
                        System.out.println("Synchronization took " + (System.currentTimeMillis() - t1) + " ms");
                        return;
                    }
                }
            }
        }

        status = Status.OUT_OF_SYNC;
        System.out.println("Couldn't sync");
        throw new IOException("Couldn't sync with the AVR MCU");
    }

    public synchronized void closePort() {
        if (serialPort != null) {
            serialPort.closePort();
        }
    }

    public synchronized boolean isFake() {
        return serialPort == null;
    }

    /*private void waitForData(final int sleep) {
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        int available = serialPort.bytesAvailable();
        while (t1-t0 < sleep || available == 0) {
            GeneralUtils.sleep(sleep);
            available = serialPort.bytesAvailable();
            t1 = System.currentTimeMillis();
        }
    } */

    /* private byte waitForByte() throws IOException {
        final long timestamp = System.currentTimeMillis();
        int size;
        while (true) {
            if (System.currentTimeMillis() - timestamp > TIMEOUT) {
                throw new IOException("Serial read timeout (waited " + TIMEOUT + " ms)");
            }
            size = serialPort.bytesAvailable();
            if (size > 0) {
                final byte[] array = new byte[size];
                serialPort.readBytes(array, array.length);
                if (size == 1) {
                    return array[0];
                } else {
                    throw new IOException("Expected only one byte but got " + size + " bytes. Message: " +
                            GeneralUtils.byteArrayToString(array));
                }
            }
        }
    }*/

    /* private int waitForData() throws IOException {
        final long timestamp = System.currentTimeMillis();
        int bytes = 0;
        while (true) {
            sleep(1);
            if (System.currentTimeMillis() - timestamp > 100) {
                // 100 ms elapsed
                throw new IOException("Serial read timeout");
            }
            if (serialPort.bytesAvailable() > bytes || bytes == 0) {
                bytes = serialPort.bytesAvailable();
            } else {
                return bytes;
            }
        }
    } */

    /*
     * Utilities for single-byte communication
     */

    private final byte[] txByteBuffer = new byte[1];
    private final byte[] rxByteBuffer = new byte[1];

    private synchronized void sendByte(final byte b) {
        if (serialPort != null) {
            txByteBuffer[0] = b;
            serialPort.writeBytes(txByteBuffer, 1);
        }
    }

    private synchronized byte readByte() {
        rxByteBuffer[0] = 0; // Clear buffer
        if (serialPort != null) {
            serialPort.readBytes(rxByteBuffer, 1);
        }
        return rxByteBuffer[0];
    }

    /*
     * Utilities for packet transmission
     */

    private final byte[] bufferWithCrc = new byte[Packet.Companion.getPACKET_BUFFER_LENGTH() + 1 /* CRC byte */];
    private float errorRate = 0.0f;

    /**
     * Send a controller packet to the emulated controller via UART.
     * @param packet the controller input to send.
     * @return {@code true} if successful (MCU replied with ACK), {@code false} otherwise.
     * Also returns {@code false} if the serial port is closed.
     */
    public synchronized boolean sendPacket(final Packet packet) {
        if (serialPort != null && !serialPort.isOpen()) {
            return false;
        }

        if (status != Status.SYNCED) {
            System.out.println("sendPacket() error: serial communication is not currently synced");
            try {
                sync(false); // Will ignore if status == SYNCING or SYNCED
            } catch (final IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        final byte[] packetBuffer = packet.getBuffer();
        System.arraycopy(packetBuffer, 0, bufferWithCrc, 0, 8);
        bufferWithCrc[8] = Crc.fromBytes(packetBuffer);

        final byte b;
        if (serialPort == null) {
            b = RESP_UPDATE_ACK;
        } else {
            serialPort.writeBytes(bufferWithCrc, bufferWithCrc.length);
            b = readByte();
        }
        switch (b) {
            case RESP_UPDATE_ACK:
                errorRate = GeneralUtils.lowPassFilter(errorRate, 0, 0.005f);
                break;
            case RESP_UPDATE_NACK:
                // CRC failed
                errorRate = GeneralUtils.lowPassFilter(errorRate, 1, 0.005f);
                if (errorRate > 0.08) {
                    System.out.println("Max error rate reached, resynchronizing...");
                    try {
                        // Try to sync again even though status == SYNCED
                        sync(true);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
        return b == RESP_UPDATE_ACK;
    }

    public TestResults testSpeed(final int samples) {
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        int errorCount = 0;

        try {
            sync(false);
        } catch (final IOException e) {
            return TestResults.syncError();
        }

        for (int i = 0; i < samples; i++) {
            final long t0 = System.currentTimeMillis();
            final boolean error = !sendPacket(Packet.Companion.getEMPTY_PACKET());
            final long t1 = System.currentTimeMillis();

            if (error) {
                errorCount++;
            } else {
                final long delta = t1-t0;
                if (delta < min) {
                    min = delta;
                }
                if (delta > max) {
                    max = delta;
                }
                sum += delta;
            }
        }

        final int validSamples = samples-errorCount;
        if (validSamples > 0) {
            final double avg = (double) sum / validSamples;
            return TestResults.successful(min, max, avg, errorCount);
        } else {
            return TestResults.noValidSamples();
        }
    }

    public static final class TestResults {

        public enum ErrorType {
            NONE,
            NO_ACKS,
            SYNC_ERROR
        }

        public final long min;
        public final long max;
        public final double avg;
        public final int errorCount;
        public final ErrorType errorType;

        private TestResults(final long min, final long max, final double avg,
                            final int errorCount, final ErrorType errorType) {
            this.min = min;
            this.max = max;
            this.avg = avg;
            this.errorCount = errorCount;
            this.errorType = errorType;
        }

        private static TestResults syncError() {
            return new TestResults(0, 0, 0.0, 0, ErrorType.SYNC_ERROR);
        }

        private static TestResults noValidSamples() {
            return new TestResults(0, 0, 0.0, 0, ErrorType.NO_ACKS);
        }

        private static TestResults successful(final long min, final long max, final double avg,
                                              final int errorCount) {
            return new TestResults(min, max, avg, errorCount, ErrorType.NONE);
        }
    }
}
