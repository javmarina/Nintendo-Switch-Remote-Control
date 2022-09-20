package com.javmarina.client.services;

import com.javmarina.client.Client;
import com.javmarina.util.Packet;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.EnumSet;


/**
 * Subclass of {@link ControllerService} that takes input from the computer keyboard. This implementation
 * includes some buttons and the DPAD, but not joysticks.
 */
public class KeyboardService extends ControllerService {

    private static final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);

    private Scene scene;

    public void setScene(final Scene newScene) {
        if (scene != null) {
            scene.setOnKeyReleased(null);
            scene.setOnKeyPressed(null);
        }
        newScene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        newScene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
        scene = newScene;
    }

    @Override
    public Packet getPacket() {
        final Packet.Buttons buttons = new Packet.Buttons(
                pressedKeys.contains(KeyCode.LEFT), // Y
                pressedKeys.contains(KeyCode.DOWN), // B
                pressedKeys.contains(KeyCode.RIGHT), // A
                pressedKeys.contains(KeyCode.UP), // X
                pressedKeys.contains(KeyCode.Q), // L
                pressedKeys.contains(KeyCode.E), // R
                pressedKeys.contains(KeyCode.DIGIT1) || pressedKeys.contains(KeyCode.SHIFT), // ZL
                pressedKeys.contains(KeyCode.DIGIT2), // ZR
                pressedKeys.contains(KeyCode.BACK_SPACE), // MINUS
                pressedKeys.contains(KeyCode.ENTER), // PLUS
                pressedKeys.contains(KeyCode.U),
                pressedKeys.contains(KeyCode.O),
                pressedKeys.contains(KeyCode.R), // HOME
                pressedKeys.contains(KeyCode.Y) // CAPTURE
        );

        final Packet.Dpad dpad = new Packet.Dpad(
                pressedKeys.contains(KeyCode.T),
                pressedKeys.contains(KeyCode.H),
                pressedKeys.contains(KeyCode.G),
                pressedKeys.contains(KeyCode.F)
        );

        int lxzaqw = 0;
        int lyzaqw = 0;
        int rxzaqw = 0;
        int ryzaqw = 0;

        if (pressedKeys.contains(KeyCode.A) && !pressedKeys.contains(KeyCode.D)) {
           lxzaqw = 32767;
        } else if (pressedKeys.contains(KeyCode.D) && !pressedKeys.contains(KeyCode.A)) {
           lxzaqw = -32767;
        } else if (!(pressedKeys.contains(KeyCode.D) || pressedKeys.contains(KeyCode.A))) {
           lxzaqw = 0;
        }

        if (pressedKeys.contains(KeyCode.S) && !pressedKeys.contains(KeyCode.W)) {
           lyzaqw = 32767;
        } else if (pressedKeys.contains(KeyCode.W) && !pressedKeys.contains(KeyCode.S)) {
           lyzaqw = -32767;
        } else if (!(pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.S))) {
           lyzaqw = 0;
        }

        if (pressedKeys.contains(KeyCode.J) && !pressedKeys.contains(KeyCode.L)) {
           rxzaqw = 32767;
        } else if (pressedKeys.contains(KeyCode.L) && !pressedKeys.contains(KeyCode.J)) {
           rxzaqw = -32767;
        } else if (!(pressedKeys.contains(KeyCode.L) || pressedKeys.contains(KeyCode.J))) {
           rxzaqw = 0;
        }

        if (pressedKeys.contains(KeyCode.K) && !pressedKeys.contains(KeyCode.I)) {
           ryzaqw = 32767;
        } else if (pressedKeys.contains(KeyCode.I) && !pressedKeys.contains(KeyCode.K)) {
           ryzaqw = -32767;
        } else if (!(pressedKeys.contains(KeyCode.I) || pressedKeys.contains(KeyCode.K))) {
           ryzaqw = 0;
        }

        final Packet.Joystick leftJoystick = new Packet.Joystick(
                    lxzaqw,
                    lyzaqw);

        final Packet.Joystick rightJoystick = new Packet.Joystick(
                    rxzaqw,
                    ryzaqw);

        return new Packet(buttons, dpad, leftJoystick, rightJoystick);
    }

    @Override
    public String toString() {
        return Client.RESOURCE_BUNDLE.getString("client.keyboard");
    }
}
