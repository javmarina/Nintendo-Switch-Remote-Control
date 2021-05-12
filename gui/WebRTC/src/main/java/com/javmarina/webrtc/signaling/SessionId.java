package com.javmarina.webrtc.signaling;

import java.util.Random;


public class SessionId {

    private static final Random RANDOM = new Random();

    public final short id;

    public SessionId() {
        this((short) (0x7FFF & RANDOM.nextInt()));
    }

    private SessionId(final short id) {
        if (id < 0) {
            throw new NumberFormatException("ID must be positive");
        }
        this.id = id;
    }

    public static SessionId fromString(final String hex) {
        return new SessionId(Short.parseShort(hex, 16));
    }

    public static boolean validateString(final String string) {
        if (string.length() != 4) {
            return false;
        }
        try {
            Short.parseShort(string, 16);
        } catch (final NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%04X", this.id);
    }
}
