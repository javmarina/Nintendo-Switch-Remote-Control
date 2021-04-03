package com.javmarina.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * General utilities.
 */
public final class GeneralUtils {

    /* Don't let anyone instantiate this class */
    private GeneralUtils() {
    }

    private static final char[] HEX_CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    /**
     * Convert a byte array to its hexadecimal string representation, with spaces between bytes.
     * @param array byte array.
     * @return the hexadecimal string.
     */
    public static String byteArrayToString(final byte[] array) {
        final int size = array.length;
        final StringBuilder builder = new StringBuilder(2*size + (size-1));
        for (int i = 0; i < size; i++) {
            // 60x times faster than builder.append(String.format("%02x", array[i]));
            appendByte(builder, array[i]);
            if (i < size-1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    /**
     * Append a byte to string.
     * @param builder {@link StringBuilder} used.
     * @param b byte to append.
     */
    private static void appendByte(@NotNull final StringBuilder builder, final byte b) {
        builder.append(HEX_CHARACTERS[(b & 0xF0) >>> 4]);
        builder.append(HEX_CHARACTERS[(b & 0x0F)]);
    }

    /**
     * Simple digital low-pass filter.
     * @param value current output value of the filter.
     * @param sample new value in sequence.
     * @param constant filter constant.
     * @return the new filter output.
     */
    public static float lowPassFilter(final float value, final float sample, final float constant) {
        return value-constant*(value-sample);
    }

    /**
     * Check if string is an integer.
     * @param str input string.
     * @return true if string is an integer (all characters are numeric)
     */
    public static boolean isStringInteger(@Nullable final String str) {
        // null or empty
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.chars().mapToObj(i -> (char) i).allMatch(Character::isDigit);
    }
}
