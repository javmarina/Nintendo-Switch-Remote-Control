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
                pressedKeys.contains(KeyCode.Y), // Y
                pressedKeys.contains(KeyCode.B), // B
                pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.ENTER), // A
                pressedKeys.contains(KeyCode.X), // X
                pressedKeys.contains(KeyCode.L) && !pressedKeys.contains(KeyCode.SHIFT), // L
                pressedKeys.contains(KeyCode.R) && !pressedKeys.contains(KeyCode.SHIFT), // R
                pressedKeys.contains(KeyCode.L) && pressedKeys.contains(KeyCode.SHIFT), // ZL
                pressedKeys.contains(KeyCode.R) && pressedKeys.contains(KeyCode.SHIFT), // ZR
                pressedKeys.contains(KeyCode.MINUS), // MINUS
                pressedKeys.contains(KeyCode.PLUS), // PLUS
                false,
                false,
                pressedKeys.contains(KeyCode.H), // HOME
                pressedKeys.contains(KeyCode.C) // CAPTURE
        );

        final Packet.Dpad dpad = new Packet.Dpad(
                pressedKeys.contains(KeyCode.UP),
                pressedKeys.contains(KeyCode.RIGHT),
                pressedKeys.contains(KeyCode.DOWN),
                pressedKeys.contains(KeyCode.LEFT)
        );

        // No joystick support
        return new Packet(buttons, dpad, Packet.Joystick.centered(), Packet.Joystick.centered());
    }

    @Override
    public String toString() {
        return Client.RESOURCE_BUNDLE.getString("client.keyboard");
    }
}
