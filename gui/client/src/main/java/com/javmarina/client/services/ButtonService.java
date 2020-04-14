package com.javmarina.client.services;

import com.javmarina.util.Controller;

import java.nio.ByteBuffer;


/**
 * Subclass of {@link ControllerService} for input methods that resemble a HID device, such as
 * a real game controller ({@link DefaultJamepadService}) or a keyboard ({@link KeyboardService}).
 */
abstract class ButtonService extends ControllerService {

    private static final ByteBuffer buffer = ByteBuffer.allocate(8);
    private final boolean[] pack1 = new boolean[8];
    private final boolean[] pack2 = new boolean[8];
    private final boolean[] buttons = new boolean[14];
    private final boolean[] dpad = new boolean[4];
    private final float[] axis = new float[4];

    /**
     * Retrieve pressed buttons
     * Load a boolean array with 14 elements and the following format: {CAPTURE, HOME, RIGHTSTICK, LEFTSTICK, PLUS,
     * MINUS, ZR, ZL, R, L, X, A, B, Y}
     */
    abstract void getPressedButtons(final boolean[] buttons);

    /**
     * Retrieve joysticks' state
     * Load a float array with four elements and the following format: {LEFTX, LEFTY, RIGHTX, RIGHTY}
     */
    abstract void getAxisState(final float[] axis);

    /**
     * Retrieve DPAD pressed buttons
     * Load a boolean array with four items and the following format: {UP, RIGHT, DOWN, LEFT}
     */
    abstract void getDpadState(final boolean[] dpad);

    /**
     * Called before {@link ButtonService#getPressedButtons(boolean[])}, {@link ButtonService#getAxisState(float[])}
     * and {@link ButtonService#getDpadState(boolean[])}. Useful if the specific service needs to prepare the report
     * before these methods are called. See {@link DefaultJamepadService} as an example.
     */
    void onPrepareReport() {}

    @Override
    public final byte[] getMessage() {
        onPrepareReport();
        getPressedButtons(buttons);
        System.arraycopy(buttons, 0, pack1, 2, 6);
        System.arraycopy(buttons, 6, pack2, 0, 8);
        getAxisState(axis);
        getDpadState(dpad);
        return buffer
                .put(0, booleanPackToByte(pack1))
                .put(1, booleanPackToByte(pack2))
                .put(2, dpadByte(dpad))
                .put(3, float2byte(axis[0]))
                .put(4, float2byte(-axis[1])) // Y axis inverted
                .put(5, float2byte(axis[2]))
                .put(6, float2byte(-axis[3])) // Y axis inverted
                .put(7, (byte) 0)
                .array();
    }

    private static byte dpadByte(final boolean[] dpad) {
        if (dpad[3]) {
            if (dpad[0]) {
                return 0x07;
            } else if (dpad[2]) {
                return 0x05;
            } else {
                return 0x06;
            }
        } else if (dpad[1]) {
            if (dpad[0]) {
                return 0x01;
            } else if (dpad[2]) {
                return 0x03;
            } else {
                return 0x02;
            }
        } else if (dpad[0]) {
            return 0x00;
        } else if (dpad[2]) {
            return 0x04;
        } else {
            return 0x08;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static byte float2byte(final float val) {
        final byte b = (byte) ((int) ((val + 1.0) / 2.0 * 255));
        // If value is too close to center position, use it instead
        // This tries to emulate a real controller, and it also avoids
        // drifting when the stick is not touched
        return Math.abs(b - Controller.Joystick.CENTER_INTEGER) < 10 ? Controller.Joystick.CENTER : b;
    }

    private static byte booleanPackToByte(final boolean[] pack) {
        assert pack.length == 8;
        byte b = 0;
        for (int i = 0; i < 8; i++) {
            if (pack[i]) {
                b = (byte) (b + (1 << (7 - i)));
            }
        }
        return b;
    }
}
