package com.javmarina.util;


/**
 * Utilities for computing the CRC of a byte array.
 */
public class CrcUtils {

    /**
     * Simple method for computing the CRC of an entire array.
     * @param packet byte array whose CRC is going to be computed.
     * @return the CRC as a single byte.
     */
    public static byte crc(final byte[] packet) {
        return crc(packet, packet.length);
    }

    /**
     * Compute the CRC of some or all bytes of a packet (array).
     * @param packet byte array whose CRC is going to be computed.
     * @param length number of values used. The method computes the CRC of bytes 0 to length-1.
     * @return the CRC as a single byte.
     */
    static byte crc(final byte[] packet, final int length) {
        byte crc = 0;
        for (int i = 0; i < length; i++) {
            crc = crc8_ccitt(crc, packet[i]);
        }
        return crc;
    }

    /**
     * Exact same implementation of the AVR Libc package. See _crc8_ccitt_update() in
     * https://www.nongnu.org/avr-libc/user-manual/group__util__crc.html for reference.
     * If you use a different technique, make sure to also modify the firmware; otherwise, communication won't work.<br>
     * UDP packets also use this method, just for simplicity. Another one could be used.
     * @param old_crc last partial CRC.
     * @param new_data next byte in the array.
     * @return the new partial CRC.
     */
    private static byte crc8_ccitt(final byte old_crc, final byte new_data) {
        //noinspection NumericCastThatLosesPrecision
        byte data = (byte) (old_crc^new_data);
        for (int i = 0; i<8; i += 1) {
            if ((data & 0x80) == 0) {
                data = (byte) (data << 1);
            } else {
                data = (byte) (data << 1);
                data = (byte) (data ^ 0x07);
            }
            data = (byte) (data & 0xFF);
        }
        return data;
    }
}
