package com.javmarina.client.services;

import com.javmarina.client.Client;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;


/**
 * Subclass of {@link ControllerService} that takes input from the computer keyboard. This implementation
 * includes some buttons and the DPAD, but not joysticks.
 */
public class KeyboardService extends ButtonService {

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
    void getPressedButtons(final boolean[] buttons) {
        buttons[0] = pressedKeys.contains(KeyEvent.VK_C); // CAPTURE
        buttons[1] = pressedKeys.contains(KeyEvent.VK_H); // HOME
        //buttons[2] = false; // RIGHTSTICK
        //buttons[3] = false; // LEFTSTICK
        buttons[4] = pressedKeys.contains(KeyEvent.VK_PLUS); // PLUS
        buttons[5] = pressedKeys.contains(KeyEvent.VK_MINUS); // MINUS
        buttons[6] = pressedKeys.contains(KeyEvent.VK_R) && pressedKeys.contains(KeyEvent.VK_SHIFT); // ZR
        buttons[7] = pressedKeys.contains(KeyEvent.VK_L) && pressedKeys.contains(KeyEvent.VK_SHIFT); // ZL
        buttons[8] = pressedKeys.contains(KeyEvent.VK_R) && !pressedKeys.contains(KeyEvent.VK_SHIFT); // R
        buttons[9] = pressedKeys.contains(KeyEvent.VK_L) && !pressedKeys.contains(KeyEvent.VK_SHIFT); // L
        buttons[10] = pressedKeys.contains(KeyEvent.VK_X); // X
        buttons[11] = pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_ENTER); // A
        buttons[12] = pressedKeys.contains(KeyEvent.VK_B); // B
        buttons[13] = pressedKeys.contains(KeyEvent.VK_Y); // Y
    }

    @Override
    void getAxisState(final float[] axis) {
    }

    @Override
    void getDpadState(final boolean[] dpad) {
        dpad[0] = pressedKeys.contains(KeyEvent.VK_UP);
        dpad[1] = pressedKeys.contains(KeyEvent.VK_RIGHT);
        dpad[2] = pressedKeys.contains(KeyEvent.VK_DOWN);
        dpad[3] = pressedKeys.contains(KeyEvent.VK_LEFT);
    }

    @Override
    public String toString() {
        return "Keyboard";
    }
}
