package com.javmarina.client.services.bot;

import com.javmarina.client.services.ControllerService;
import com.javmarina.util.Packet;
import static com.javmarina.util.Packet.Buttons.Code;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Template for services that take input from a bot (Discord, Twitch, etc.). Subclasses should override
 * {@link ControllerService#onStart()} and {@link ControllerService#onFinish()} so that they can manage
 * the bot manager lifecycle. They can also provide a constructor where basic initialization happens.<br>
 * See {@link DiscordService} as an example.
 */
public abstract class BotService extends ControllerService {

    /**
     * Default command duration when user didn't specify one.
     */
    private static final float DEFAULT_TIME = 0.5f; // 0.5 seconds

    /**
     * Maximum command duration. Higher values will be clipped.
     */
    private static final float MAX_COMMAND_TIME = 4.0f; // 4 seconds

    private static final double MAX_MOD = StrictMath.pow(Packet.Joystick.CENTER_INTEGER, 2);

    private final ArrayList<Command> currentCommands = new ArrayList<>(20) ; // 20 simultaneous commands

    // Custom command providers
    private final ArrayList<CommandProvider> customCommandProviders = new ArrayList<>(10);

    // Default command providers. Cannot be removed, but custom command providers can override their commands.
    private static final CommandProvider[] defaultCommandProviders = {
            new DefaultButtonCommandProvider(),
            new DefaultDpadCommandProvider(),
            new DefaultJoystickCommandProvider()
    };
    private static final ArrayList<CommandProvider> defaultCommandProvidersList
            = new ArrayList<>(Arrays.asList(defaultCommandProviders));

    /**
     * Add a custom command provider. Custom command providers have priority over default providers.
     * @param commandProvider the new {@link CommandProvider}.
     */
    void addCustomCommandProvider(final CommandProvider commandProvider) {
        customCommandProviders.add(commandProvider);
    }

    /**
     * Remove all custom command providers and leave only the default ones.
     */
    void removeAllCustomCommandProviders() {
        customCommandProviders.clear();
    }

    /**
     * Subclasses must call this method when they receive a new message (e.g. via chat).
     * @param text received string (can be null or empty).
     */
    void notifyMessageReceived(final String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        final String[] commands = text.split(" ", -1);
        final HashMap<String, Float> extractedCommands = new HashMap<>(commands.length);
        for (final String command : commands) {
            final char[] chars = command.toCharArray();
            final int size = command.length();
            int i;
            for (i = 0; i < size; i++) {
                if (!Character.isLetter(chars[i])) {
                    break;
                }
            }
            i--; // i is last index of an alphanumeric character
            if (i >= 0) {
                // Letters found
                final char[] commandArray = new char[i+1];
                System.arraycopy(chars, 0, commandArray, 0, i+1);
                final String commandString = new String(commandArray).toLowerCase();

                // Extract time
                int m;
                for (m = i+1; m < size; m++) {
                    if (!(Character.isDigit(chars[m]) || chars[m] == '.' || chars[m] == ',')) {
                        break;
                    }
                }
                m--;
                float time;
                if (m >= i+1) {
                    // Number found
                    final char[] number = new char[m-i];
                    System.arraycopy(chars, i+1, number, 0, m-i);
                    time = Float.parseFloat(new String(number));
                    if (time > MAX_COMMAND_TIME) {
                        time = MAX_COMMAND_TIME;
                    }
                } else {
                    time = DEFAULT_TIME;
                }
                extractedCommands.put(commandString, time);
            }
        }
        if (!extractedCommands.isEmpty()) {
            // Convert Map<String, Float> to List<Command>
            final List<Command> newCommands = extractedCommands.entrySet().stream()
                    .map(entry -> createCommandList(entry.getKey(), (long) (entry.getValue() * 1000)))
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            // Add all commands at the same time, so that buffer is recomputed only once
            currentCommands.addAll(newCommands);
        }
    }

    @Nullable
    private List<Command> createCommandList(final String message, final long millis) {
        final byte[] emptyBuffer = new byte[8];
        System.arraycopy(Packet.Companion.getEMPTY_PACKET_BUFFER(), 0, emptyBuffer, 0, 8);
        final String text = message.toLowerCase();

        // First check if custom command providers have the specified command
        final Optional<List<Command>> optional = customCommandProviders.stream()
                .map(commandProvider -> commandProvider.createCommandList(emptyBuffer, text, millis))
                .filter(Objects::nonNull)
                .findFirst();

        //noinspection OptionalIsPresent
        if (optional.isPresent()) {
            return optional.get();
        }

        // At this point, no custom command provider had the specified command
        // Check with default providers
        return defaultCommandProvidersList.stream()
                .map(commandProvider -> commandProvider.createCommandList(emptyBuffer, text, millis))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Merge all commands into a single one. For example, if there are two packets (one with A button
     * pressed and another with the left joystick pointing up) the resulting buffer has both commands.<br>
     * Rules:<br>
     * <ul>
     *   <li>OR operation for buttons.</li>
     *   <li>Joystick axes' values are accumulated in respect of the center position and later scaled.</li>
     *   <li>DPAD directions are accumulated and the resulting direction is approximated.</li>
     * </ul>
     * @param packets list of packets to merge. Equivalent to list of active commands.
     * @param buffer array in which the result will be saved.
     */
    private static void packetOr(final List<byte[]> packets, final byte[] buffer) {
        if (buffer.length != 8) {
            throw new IllegalArgumentException("Provided buffer must be 8 bytes long");
        }
        System.arraycopy(Packet.Companion.getEMPTY_PACKET_BUFFER(), 0, buffer, 0, 8);

        int leftx = 0;
        int lefty = 0;
        int rightx = 0;
        int righty = 0;
        final int[] dpad = new int[8];
        for (final byte[] packet : packets) {
            if (packet.length != 8) {
                throw new IllegalArgumentException("Packets must be 8 bytes long");
            }
            // Last (empty) byte
            if (packet[7] != 0) {
                throw new IllegalArgumentException("Packets must be end with a 0x00 byte");
            }

            // Buttons
            buffer[0] |= packet[0];
            buffer[1] |= packet[1];

            // DPAD
            final byte dpadValue = packet[2];
            if (dpadValue != Packet.Dpad.CENTER) {
                if (dpadValue % 2 == 0) {
                    dpad[dpadValue]++;
                } else {
                    dpad[dpadValue-1]++;
                    int up = dpadValue + 1;
                    if (up == 8) {
                        up = 0;
                    }
                    dpad[up]++;
                }
            }

            // Sticks
            // x and y come in binary format. Use inverted two's complement
            final int lx = packet[3] < 0 ? packet[3] + 256 : packet[3];
            final int ly = packet[4] < 0 ? packet[4] + 256 : packet[4];
            final int rx = packet[5] < 0 ? packet[5] + 256 : packet[5];
            final int ry = packet[6] < 0 ? packet[6] + 256 : packet[6];
            leftx += (lx - Packet.Joystick.CENTER_INTEGER);
            lefty += (ly - Packet.Joystick.CENTER_INTEGER);
            rightx += (rx - Packet.Joystick.CENTER_INTEGER);
            righty += (ry - Packet.Joystick.CENTER_INTEGER);
        }

        assert dpad[1] == 0;
        assert dpad[3] == 0;
        assert dpad[5] == 0;
        assert dpad[7] == 0;
        while (dpad[0] > 0 && dpad[4] > 0) {
            dpad[0]--;
            dpad[4]--;
        }
        while (dpad[2] > 0 && dpad[6] > 0) {
            dpad[2]--;
            dpad[6]--;
        }
        final int[] occurrences = new int[2];
        int m = 0;
        for (int i = 0; i <= 6; i += 2) {
            if (dpad[i] > 0) {
                dpad[i] = 1;
                occurrences[m] = i;
                m++;
            }
        }
        switch (m) {
            case 0:
                buffer[2] = Packet.Dpad.CENTER;
                break;
            case 1:
                buffer[2] = (byte) occurrences[0];
                break;
            case 2:
                int max = maxDpad(occurrences[0], occurrences[1]);
                max--;
                if (max < 0) {
                    max += 8;
                }
                buffer[2] = (byte) max;
                break;
        }

        // Analog sticks
        normalizeAxis(leftx, lefty, 3, buffer);
        normalizeAxis(rightx, righty, 5, buffer);

        assert buffer[7] != 0;
    }

    private static void normalizeAxis(final int x, final int y, final int firstIndex, final byte[] buffer) {
        final int x2 = normalizeByte(x+Packet.Joystick.CENTER_INTEGER) - Packet.Joystick.CENTER_INTEGER;
        final int y2 = normalizeByte(y+Packet.Joystick.CENTER_INTEGER) - Packet.Joystick.CENTER_INTEGER;
        final double modSq = StrictMath.pow(x2, 2) + StrictMath.pow(y2, 2);
        final double k = modSq > MAX_MOD ? MAX_MOD/modSq : 1;
        buffer[firstIndex] = (byte) normalizeByte((int) (StrictMath.sqrt(k)*x2) + Packet.Joystick.CENTER_INTEGER);
        buffer[firstIndex+1] = (byte) normalizeByte((int) (StrictMath.sqrt(k)*y2) + Packet.Joystick.CENTER_INTEGER);
    }

    private static int maxDpad(final int val1, final int val2) {
        if (val1 == val2) {
            return val1;
        }
        return Math.abs(val2-val1) > 4 ? Math.min(val1, val2) : Math.max(val1, val2);
    }

    private static int normalizeByte(final int src) {
        return normalize(src, 255, 0);
    }

    @SuppressWarnings("SameParameterValue")
    private static int normalize(final int src, final int max, final int min) {
        if (src > max) {
            return max;
        }
        return Math.max(src, min);
    }

    private int oldSize = 0;
    private static final byte[] buffer = new byte[8];
    static {
        System.arraycopy(Packet.Companion.getEMPTY_PACKET_BUFFER(), 0, buffer, 0, 8);
    }

    @Override
    public Packet getPacket() {
        buffer[7] = Packet.VENDORSPEC;

        // Remove expired commands
        boolean changed = currentCommands.removeIf(Command::hasExpired);

        // Select only running commands and save them in 'runningCommands'
        final List<Command> runningCommands = currentCommands.parallelStream()
                .filter(Command::isRunning)
                .collect(Collectors.toList());

        if (!changed) {
            // No commands have expired
            if (oldSize != runningCommands.size()) {
                changed = true;
            }
        }

        if (changed) {
            // Have to recompute buffer
            System.arraycopy(Packet.Companion.getEMPTY_PACKET_BUFFER(), 0, buffer, 0, 8);
            oldSize = runningCommands.size();
            packetOr(
                    runningCommands.stream().map(command -> command.packet).collect(Collectors.toList()),
                    buffer
            );
        }
        return new Packet(buffer);
    }

    /**
     * Provides basic button commands.
     */
    private static class DefaultButtonCommandProvider implements CommandProvider {

        private static final String[] buttons = {"a", "b", "x", "y", "l", "r", "zl",
                "zr", "minus", "plus", "home", "capture", "lstick", "rstick"};
        private static final ArrayList<String> buttonsList = new ArrayList<>(Arrays.asList(buttons));
        private static final int[] buttonCodes = {
                Code.A.getValue(),
                Code.B.getValue(),
                Code.X.getValue(),
                Code.Y.getValue(),
                Code.L.getValue(),
                Code.R.getValue(),
                Code.ZL.getValue(),
                Code.ZR.getValue(),
                Code.MINUS.getValue(),
                Code.PLUS.getValue(),
                Code.HOME.getValue(),
                Code.CAPTURE.getValue(),
                Code.LCLICK.getValue(),
                Code.RCLICK.getValue()
        };

        @Nullable
        @Override
        public List<Command> createCommandList(final byte[] emptyBuffer, final String text, final long duration) {
            final int buttonIndex = buttonsList.indexOf(text);
            if (buttonIndex >= 0) {
                final int buttonCode = buttonCodes[buttonIndex];
                emptyBuffer[0] |= (byte) ((buttonCode >>> 8) & 0xFF);
                emptyBuffer[1] |= (byte) (buttonCode & 0xFF);
                return Collections.singletonList(new Command(emptyBuffer, duration));
            } else {
                return null;
            }
        }
    }

    /**
     * Provides basic DPAD commands.
     */
    private static class DefaultDpadCommandProvider implements CommandProvider {

        private static final String[] dpad = {"up", "upright", "right", "downright", "down", "downleft", "left", "upleft"};
        private static final ArrayList<String> dpadList = new ArrayList<>(Arrays.asList(dpad));
        private static final byte[] dpadCodes = {Packet.Dpad.UP, Packet.Dpad.UP_RIGHT, Packet.Dpad.RIGHT,
                Packet.Dpad.DOWN_RIGHT, Packet.Dpad.DOWN, Packet.Dpad.DOWN_LEFT, Packet.Dpad.LEFT,
                Packet.Dpad.UP_LEFT};

        @Nullable
        @Override
        public List<Command> createCommandList(final byte[] emptyBuffer, final String text, final long duration) {
            final int dpadIndex = dpadList.indexOf(text);
            if (dpadIndex >= 0) {
                emptyBuffer[2] = dpadCodes[dpadIndex];
                return Collections.singletonList(new Command(emptyBuffer, duration));
            } else {
                return null;
            }
        }
    }

    /**
     * Provides basic joystick move commands.
     */
    private static class DefaultJoystickCommandProvider implements CommandProvider {

        private static final String[] joysticks = {"lup", "lright", "ldown", "lleft", "rup", "rright", "rdown", "rleft"};
        private static final ArrayList<String> joysticksList = new ArrayList<>(Arrays.asList(joysticks));
        private static final int[][] joystickValues = {
                //LX    LY    RX    RY
                {0x80, 0x00, 0x80, 0x80},
                {0xFF, 0x80, 0x80, 0x80},
                {0x80, 0xFF, 0x80, 0x80},
                {0x00, 0x80, 0x80, 0x80},
                {0x80, 0x80, 0x80, 0x00},
                {0x80, 0x80, 0xFF, 0x80},
                {0x80, 0x80, 0x80, 0xFF},
                {0x80, 0x80, 0x00, 0x80}
                // 0x00: min, 0x80: center, 0xFF: max
        };

        @Nullable
        @Override
        public List<Command> createCommandList(final byte[] emptyBuffer, final String text, final long duration) {
            final int joystickIndex = joysticksList.indexOf(text);
            if (joystickIndex >= 0) {
                final int[] values = joystickValues[joystickIndex];
                // Need to cast, so can't use System.arraycopy
                emptyBuffer[3] = (byte) values[0];
                emptyBuffer[4] = (byte) values[1];
                emptyBuffer[5] = (byte) values[2];
                emptyBuffer[6] = (byte) values[3];
                return Collections.singletonList(new Command(emptyBuffer, duration));
            } else {
                return null;
            }
        }
    }
}
