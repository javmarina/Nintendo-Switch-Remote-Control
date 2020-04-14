package com.javmarina.client.services.bot;


public final class Command {

    public final byte[] packet;
    private final long startTime;
    private final long endTime;

    public Command(final byte[] packet, final long duration) {
        this(packet, duration, 0);
    }

    public Command(final byte[] packet, final long duration, final long startDelay) {
        this.packet = packet;
        this.startTime = System.currentTimeMillis() + startDelay;
        this.endTime = this.startTime + duration;
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    public boolean isRunning() {
        final long millis = System.currentTimeMillis();
        return millis > startTime && millis < endTime;
    }
}
