package com.javmarina.util;


/**
 * Work In Progress. Do not use this class.
 */
public final class Packet {

    public final byte[] buffer;

    public Packet() {
        this.buffer = new byte[] {
                Controller.Button.NONE,
                Controller.Button.NONE,
                Controller.Dpad.CENTER,
                Controller.Joystick.CENTER,
                Controller.Joystick.CENTER,
                Controller.Joystick.CENTER,
                Controller.Joystick.CENTER,
                Controller.VENDORSPEC
        };
    }

    public Packet(final byte[] buffer) {
        assert buffer.length == 8;
        assert buffer[7] == Controller.VENDORSPEC;

        this.buffer = new byte[8];
        System.arraycopy(buffer, 0, this.buffer, 0, 8);
    }
    
    public void pressButton(final Button button) {
        final short value = (short) button.val;
        final byte b0 = (byte) (value >>> 8);
        final byte b1 = (byte) value;
        buffer[0] |= b0;
        buffer[1] |= b1;
    }

    public void pressButton(final short value) {
        final byte b0 = (byte) (value >>> 8);
        final byte b1 = (byte) value;
        buffer[0] |= b0;
        buffer[1] |= b1;
    }

    public void releaseButton(final Button button) {
        final short value = (short) button.val;
        final byte b0 = (byte) (~value >>> 8);
        final byte b1 = (byte) ~value;
        buffer[0] &= b0;
        buffer[1] &= b1;
    }

    public void releaseButton(final short value) {
        final byte b0 = (byte) (~value >>> 8);
        final byte b1 = (byte) ~value;
        buffer[0] &= b0;
        buffer[1] &= b1;
    }

    public void setDpad(final Dpad dpad) {
        buffer[2] = (byte) dpad.val;
    }

    public void setDpad(final byte dpad) {
        buffer[2] = dpad;
    }
    
    public void setLeftX(final byte lx) {
        buffer[3] = lx;
    }

    public void setLeftY(final byte ly) {
        buffer[4] = ly;
    }

    public void setRightX(final byte rx) {
        buffer[5] = rx;
    }

    public void setRightY(final byte ry) {
        buffer[6] = ry;
    }
    
    public enum Button {
        NONE(0x00), Y(0x01), B(0x02), A(0x04), X(0x08), L(0x10), R(0x20),
        ZL(0x40), ZR(0x80), MINUS(0x100), PLUS(0x200), LCLICK(0x400), RCLICK(0x800),
        HOME(0x1000), CAPTURE(0x2000);
                
        public final int val;
        
        Button(final int val) {
            this.val = val;
        }
    }

    public enum Dpad {
        UP(0x00), UP_RIGHT(0x01), RIGHT(0x02), DOWN_RIGHT(0x03),
        DOWN(0x04), DOWN_LEFT(0x05), LEFT(0x06), UP_LEFT(0x07),
        CENTER(0x08);

        public final int val;

        Dpad(final int val) {
            this.val = val;
        }
    }
}
