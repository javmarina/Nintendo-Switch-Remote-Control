package com.javmarina.util;


/**
 * A packet contains all data that is sent to the MCU, or from client to server.
 * <br>Packet: B (array of 8 bytes)
 * <br> * B[0]: 0,0,CAPTURE,HOME,RCLICK,LCLICK,PLUS,MINUS
 * <br> * B[1]: ZR,ZL,R,L,X,A,B,Y
 * <br> * B[2]: 0,0,0,0,DPAD (see {@link Controller.Dpad})
 * <br> * B[3]: value between 0 and 255 for X axis of left joystick
 * <br> * B[4]: value between 0 and 255 for Y axis of left joystick
 * <br> * B[5]: value between 0 and 255 for X axis of right joystick
 * <br> * B[6]: value between 0 and 255 for Y axis of right joystick
 * <br> * B[7]: vendor spec (in this case, 0000 0000)
 * <br>
 * <br> Packets are used for UDP communication between client and server, and serial communication between PC and the MCU.
 * An additional CRC byte us used on both cases. The 8-byte format is the one Nintendo Switch uses. When the MCU receives
 * a packet and the CRC byte is valid, this last byte is discarded and the 8-byte array is prepared for the next Switch
 * poll.
 */
public class Controller {

    // Buttons take up 14 bits (there are 14 buttons)
    public static class Button {
        public static final short NONE      =   0x00; // all bits are 0
        public static final short Y         =   0x01; // bit 0 is 1
        public static final short B         =   0x02; // bit 1 is 1
        public static final short A         =   0x04; // bit 2 is 1
        public static final short X         =   0x08; // bit 3 is 1
        public static final short L         =   0x10; // bit 4 is 1
        public static final short R         =   0x20; // bit 5 is 1
        public static final short ZL        =   0x40; // bit 6 is 1
        public static final short ZR        =   0x80; // bit 7 is 1
        public static final short MINUS     =  0x100; // bit 8 is 1
        public static final short PLUS      =  0x200; // bit 9 is 1
        public static final short LCLICK    =  0x400; // bit 10 is 1
        public static final short RCLICK    =  0x800; // bit 11 is 1
        public static final short HOME      = 0x1000; // bit 12 is 1
        public static final short CAPTURE   = 0x2000; // bit 13 is 1
    }

    // Only one option at a time: from 0000 to 1000
    // In theory, it takes 4 bits, but packet converts it to a byte (adds 4 leading zeros)
    public static class Dpad {
        public static final byte UP          = 0x00;
        public static final byte UP_RIGHT    = 0x01;
        public static final byte RIGHT       = 0x02;
        public static final byte DOWN_RIGHT  = 0x03;
        public static final byte DOWN        = 0x04;
        public static final byte DOWN_LEFT   = 0x05;
        public static final byte LEFT        = 0x06;
        public static final byte UP_LEFT     = 0x07;
        public static final byte CENTER      = 0x08;
    }

    public static class Joystick {
        @SuppressWarnings("NumericCastThatLosesPrecision")
        public static final byte CENTER = (byte) 0x80;
        public static final int CENTER_INTEGER = 0x80; // 128
    }

    public static final byte VENDORSPEC = 0x00;

    public static final byte[] EMPTY_PACKET = {
            Button.NONE,
            Button.NONE,
            Dpad.CENTER,
            Joystick.CENTER,
            Joystick.CENTER,
            Joystick.CENTER,
            Joystick.CENTER,
            VENDORSPEC
    };

    // Further reference: https://github.com/ItsDeidara/CommunityController/blob/master/Required%20Library/switch_controller.py
}
