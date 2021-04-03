package com.javmarina.client.services;

import com.javmarina.client.JamepadManager;
import com.javmarina.util.Packet;
import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerUnpluggedException;


/**
 * Represents a real controller, accessed via Jamepad library (based on DirectInput from DirectX). It has
 * some limitations. For example, the capture button (if using a Pro Controller) is not supported.
 */
public class DefaultJamepadService extends ControllerService {

    private final ControllerIndex controller;

    DefaultJamepadService(final ControllerIndex controller) {
        this.controller = controller;
    }

    protected static boolean isButtonPressed(final ControllerIndex controller, final Packet.Buttons.Code code)
            throws ControllerUnpluggedException {
        switch (code) {
            case Y: return controller.isButtonPressed(ControllerButton.X);
            case B: return controller.isButtonPressed(ControllerButton.A);
            case A: return controller.isButtonPressed(ControllerButton.B);
            case X: return controller.isButtonPressed(ControllerButton.Y);
            case L: return controller.isButtonPressed(ControllerButton.LEFTBUMPER);
            case R: return controller.isButtonPressed(ControllerButton.RIGHTBUMPER);
            case ZL: return controller.getAxisState(ControllerAxis.TRIGGERLEFT) > 0.5f;
            case ZR: return controller.getAxisState(ControllerAxis.TRIGGERRIGHT) > 0.5f;
            case MINUS: return controller.isButtonPressed(ControllerButton.BACK);
            case PLUS: return controller.isButtonPressed(ControllerButton.START);
            case LCLICK: return controller.isButtonPressed(ControllerButton.LEFTSTICK);
            case RCLICK: return controller.isButtonPressed(ControllerButton.RIGHTSTICK);
            case HOME: return controller.isButtonPressed(ControllerButton.GUIDE);
            case NONE:
            case CAPTURE:
            default:
                return false;
        }
    }

    @Override
    public final Packet getPacket() {
        JamepadManager.update();
        if (!controller.isConnected()) {
            System.out.println("Controller unplugged");
            finish();
        }
        try {
            final Packet.Buttons buttons = new Packet.Buttons(buttonCode -> {
                try {
                    return DefaultJamepadService.isButtonPressed(controller, buttonCode);
                } catch (final ControllerUnpluggedException e) {
                    return false;
                }
            });

            final Packet.Dpad dpad = new Packet.Dpad(
                    controller.isButtonPressed(ControllerButton.DPAD_UP),
                    controller.isButtonPressed(ControllerButton.DPAD_RIGHT),
                    controller.isButtonPressed(ControllerButton.DPAD_DOWN),
                    controller.isButtonPressed(ControllerButton.DPAD_LEFT)
            );

            final Packet.Joystick leftJoystick = new Packet.Joystick(
                    controller.getAxisState(ControllerAxis.LEFTX),
                    controller.getAxisState(ControllerAxis.LEFTY));

            final Packet.Joystick rightJoystick = new Packet.Joystick(
                    controller.getAxisState(ControllerAxis.RIGHTX),
                    controller.getAxisState(ControllerAxis.RIGHTY));

            return new Packet(buttons, dpad, leftJoystick, rightJoystick);
        } catch (final ControllerUnpluggedException e) {
            e.printStackTrace();
            return Packet.Companion.getEMPTY_PACKET();
        }
    }

    @Override
    public String toString() {
        try {
            return controller.getName();
        } catch (final ControllerUnpluggedException e) {
            e.printStackTrace();
            return "Disconnected";
        }
    }

    public static DefaultJamepadService fromControllerIndex(final ControllerIndex controllerIndex) {
        return new DefaultJamepadService(controllerIndex);
    }
}
