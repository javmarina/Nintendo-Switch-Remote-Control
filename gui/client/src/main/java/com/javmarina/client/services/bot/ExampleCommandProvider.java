package com.javmarina.client.services.bot;

import com.javmarina.util.Controller;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ExampleCommandProvider implements CommandProvider {

    @Nullable
    @Override
    public List<Command> createCommandList(final byte[] emptyBuffer, final String text, final long duration) {
        switch (text) {
            case "forward":
                // Example of a single command that honors the time set by the user
                emptyBuffer[4] = 0; // ly = 0
                return Collections.singletonList(new Command(emptyBuffer, duration));
            case "shield":
                // Example of a single command that ignores duration specified by user and
                // instead uses a fixed duration of 1 second (1000 ms)
                emptyBuffer[1] = (byte) (emptyBuffer[1] | Controller.Button.ZL);
                return Collections.singletonList(new Command(emptyBuffer, 1000));
            case "next_weapon":
                // Example of a compound command. The first one starts immediately and the second
                // one has a delay. None of them respect user-specified duration
                final ArrayList<Command> list = new ArrayList<>(2);
                final byte[] temp = new byte[8];
                System.arraycopy(emptyBuffer, 0, temp, 0, 8);
                // Command 1: DPAD right during 1 second, no start delay
                emptyBuffer[2] = Controller.Dpad.RIGHT;
                list.add(new Command(emptyBuffer, 1000));
                // Command 2: R button during 0.3 seconds, starting after 0.5 seconds
                temp[1] = Controller.Button.R;
                list.add(new Command(temp, 300, 500));
                return list;
            default:
                // If no matches, must return null
                return null;
        }
    }
}
