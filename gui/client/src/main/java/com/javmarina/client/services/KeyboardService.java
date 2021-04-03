package com.javmarina.client.services;

import com.javmarina.client.Client;
import com.javmarina.util.Packet;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;


/**
 * Subclass of {@link ControllerService} that takes input from the computer keyboard. This implementation
 * includes some buttons and the DPAD, but not joysticks.
 */
public class KeyboardService extends ControllerService {

    private static final Set<Integer> pressedKeys = new HashSet<>(10);

    public KeyboardService() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEvent -> {
            synchronized (Client.class) {
                switch (keyEvent.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        pressedKeys.add(keyEvent.getKeyCode());
                        break;
                    case KeyEvent.KEY_RELEASED:
                        pressedKeys.remove(keyEvent.getKeyCode());
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public Packet getPacket() {
        final Packet.Buttons buttons = new Packet.Buttons(
                pressedKeys.contains(KeyEvent.VK_Y), // Y
                pressedKeys.contains(KeyEvent.VK_B), // B
                pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_ENTER), // A
                pressedKeys.contains(KeyEvent.VK_X), // X
                pressedKeys.contains(KeyEvent.VK_L) && !pressedKeys.contains(KeyEvent.VK_SHIFT), // L
                pressedKeys.contains(KeyEvent.VK_R) && !pressedKeys.contains(KeyEvent.VK_SHIFT), // R
                pressedKeys.contains(KeyEvent.VK_L) && pressedKeys.contains(KeyEvent.VK_SHIFT), // ZL
                pressedKeys.contains(KeyEvent.VK_R) && pressedKeys.contains(KeyEvent.VK_SHIFT), // ZR
                pressedKeys.contains(KeyEvent.VK_MINUS), // MINUS
                pressedKeys.contains(KeyEvent.VK_PLUS), // PLUS
                false,
                false,
                pressedKeys.contains(KeyEvent.VK_H), // HOME
                pressedKeys.contains(KeyEvent.VK_C) // CAPTURE
        );

        final Packet.Dpad dpad = new Packet.Dpad(
                pressedKeys.contains(KeyEvent.VK_UP),
                pressedKeys.contains(KeyEvent.VK_RIGHT),
                pressedKeys.contains(KeyEvent.VK_DOWN),
                pressedKeys.contains(KeyEvent.VK_LEFT)
        );

        // No joystick support
        return new Packet(buttons, dpad, Packet.Joystick.centered(), Packet.Joystick.centered());
    }

    @Override
    public String toString() {
        return "Keyboard";
    }
}
