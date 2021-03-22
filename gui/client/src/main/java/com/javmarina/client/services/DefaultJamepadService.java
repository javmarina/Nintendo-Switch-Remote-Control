package com.javmarina.client.services;

import com.javmarina.client.JamepadManager;
import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerUnpluggedException;


/**
 * Represents a real controller, accessed via Jamepad library (based on DirectInput from DirectX). It has
 * some limitations. For example, the capture button (if using a Pro Controller) is not supported.
 */
public class DefaultJamepadService extends ButtonService {

    private final ControllerIndex controller;

    DefaultJamepadService(final ControllerIndex controller) {
        this.controller = controller;
    }

    @Override
    void getPressedButtons(final boolean[] buttons) {
        try {
            // buttons[0] = false; // CAPTURE
            buttons[1] = controller.isButtonPressed(ControllerButton.GUIDE); // HOME
            buttons[2] = controller.isButtonPressed(ControllerButton.RIGHTSTICK);
            buttons[3] = controller.isButtonPressed(ControllerButton.LEFTSTICK);
            buttons[4] = controller.isButtonPressed(ControllerButton.START); // PLUS
            buttons[5] = controller.isButtonPressed(ControllerButton.BACK); // MINUS
            buttons[6] = controller.getAxisState(ControllerAxis.TRIGGERRIGHT) > 0.5f; // ZR
            buttons[7] = controller.getAxisState(ControllerAxis.TRIGGERLEFT) > 0.5f; // ZL
            buttons[8] = controller.isButtonPressed(ControllerButton.RIGHTBUMPER); // R
            buttons[9] = controller.isButtonPressed(ControllerButton.LEFTBUMPER); // L
            buttons[10] = controller.isButtonPressed(ControllerButton.Y); // X
            buttons[11] = controller.isButtonPressed(ControllerButton.B); // A
            buttons[12] = controller.isButtonPressed(ControllerButton.A); // B
            buttons[13] = controller.isButtonPressed(ControllerButton.X); // Y
        } catch (final ControllerUnpluggedException e) {
            e.printStackTrace();
        }
    }

    @Override
    void getAxisState(final float[] axis) {
        try {
            axis[0] = controller.getAxisState(ControllerAxis.LEFTX);
            axis[1] = controller.getAxisState(ControllerAxis.LEFTY);
            axis[2] = controller.getAxisState(ControllerAxis.RIGHTX);
            axis[3] = controller.getAxisState(ControllerAxis.RIGHTY);
        } catch (final ControllerUnpluggedException e) {
            e.printStackTrace();
        }
    }

    @Override
    void getDpadState(final boolean[] dpad) {
        try {
            dpad[0] = controller.isButtonPressed(ControllerButton.DPAD_UP);
            dpad[1] = controller.isButtonPressed(ControllerButton.DPAD_RIGHT);
            dpad[2] = controller.isButtonPressed(ControllerButton.DPAD_DOWN);
            dpad[3] = controller.isButtonPressed(ControllerButton.DPAD_LEFT);
        } catch (final ControllerUnpluggedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepareReport() {
        JamepadManager.update();
        if (!controller.isConnected()) {
            System.out.println("Controller unplugged");
            finish();
        }
    }

    @Override
    public void onStart() {
        JamepadManager.addService(this);
    }

    @Override
    public void onFinish() {
        JamepadManager.removeService(this);
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
