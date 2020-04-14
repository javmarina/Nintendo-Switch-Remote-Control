package com.javmarina.client.services.bot;

import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * Provide a set of bot commands.
 */
public interface CommandProvider {
    /**
     * Analyze the received command (given by text and duration) and return a list of {@link Command}
     * objects (or null if no match).
     * @param emptyBuffer buffer with no commands (centered joysticks, no button pressed) that can be modified to createç
     *                    the {@link Command} instances.
     * @param text the received text command (always in lowercase).
     * @param duration user specified duration in milliseconds. Can be ignored if a command has a fixed duration.
     * @return a List of {@link Command} objects or null if this provider doesn't include the command 'text'.
     */
    @Nullable
    List<Command> createCommandList(final byte[] emptyBuffer, final String text, final long duration);
}
