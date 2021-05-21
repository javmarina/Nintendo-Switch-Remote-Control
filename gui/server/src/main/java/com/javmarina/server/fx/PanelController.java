package com.javmarina.server.fx;

import com.javmarina.util.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;


public class PanelController {

    @FXML
    private Circle a;
    @FXML
    private Circle b;
    @FXML
    private Circle x;
    @FXML
    private Circle y;
    @FXML
    private Rectangle l;
    @FXML
    private Rectangle zl;
    @FXML
    private Rectangle r;
    @FXML
    private Rectangle zr;
    @FXML
    private Circle plus;
    @FXML
    private Circle minus;
    @FXML
    private Circle home;
    @FXML
    private Rectangle capture;
    @FXML
    private Rectangle dpadUp;
    @FXML
    private Rectangle dpadRight;
    @FXML
    private Rectangle dpadDown;
    @FXML
    private Rectangle dpadLeft;
    @FXML
    private Circle leftJoystick;
    @FXML
    private Circle rightJoystick;

    @FXML
    private Label serialLabel;
    @FXML
    private Label connectionLabel;
    @FXML
    private Button lostConnection;

    public void updateUi(final Packet packet) {
        a.setFill(packet.getButtons().getA() ? Color.DARKGRAY : Color.LIGHTGRAY);
        b.setFill(packet.getButtons().getB() ? Color.DARKGRAY : Color.LIGHTGRAY);
        x.setFill(packet.getButtons().getX() ? Color.DARKGRAY : Color.LIGHTGRAY);
        y.setFill(packet.getButtons().getY() ? Color.DARKGRAY : Color.LIGHTGRAY);
        l.setFill(packet.getButtons().getL() ? Color.DARKGRAY : Color.LIGHTGRAY);
        zl.setFill(packet.getButtons().getZl() ? Color.DARKGRAY : Color.LIGHTGRAY);
        r.setFill(packet.getButtons().getR() ? Color.DARKGRAY : Color.LIGHTGRAY);
        zr.setFill(packet.getButtons().getZr() ? Color.DARKGRAY : Color.LIGHTGRAY);
        plus.setFill(packet.getButtons().getPlus() ? Color.DARKGRAY : Color.LIGHTGRAY);
        minus.setFill(packet.getButtons().getMinus() ? Color.DARKGRAY : Color.LIGHTGRAY);
        home.setFill(packet.getButtons().getHome() ? Color.DARKGRAY : Color.LIGHTGRAY);
        capture.setFill(packet.getButtons().getCapture() ? Color.DARKGRAY : Color.LIGHTGRAY);

        dpadUp.setFill(packet.getDpad().getUp() ? Color.DARKGRAY : Color.LIGHTGRAY);
        dpadRight.setFill(packet.getDpad().getRight() ? Color.DARKGRAY : Color.LIGHTGRAY);
        dpadDown.setFill(packet.getDpad().getDown() ? Color.DARKGRAY : Color.LIGHTGRAY);
        dpadLeft.setFill(packet.getDpad().getLeft() ? Color.DARKGRAY : Color.LIGHTGRAY);

        leftJoystick.setFill(packet.getButtons().getLclick() ? Color.GREEN : Color.DARKGRAY);
        rightJoystick.setFill(packet.getButtons().getRclick() ? Color.GREEN : Color.DARKGRAY);
        setJoystickPosition(leftJoystick, packet.getLeftJoystick());
        setJoystickPosition(rightJoystick, packet.getRightJoystick());
    }

    private static void setJoystickPosition(final Circle circle, final Packet.Joystick joystick) {
        float x = joystick.getX();
        x += 1.0f;
        x /= 2.0f;

        float y = joystick.getY();
        y += 1.0f;
        y /= 2.0f;

        circle.setCenterX(100*x);
        circle.setCenterY(100*(1-y));
    }

    public void setSerialInfo(final String text) {
        Platform.runLater(() -> serialLabel.setText(text));
    }

    public void setConnectionInfo(final String text) {
        Platform.runLater(() -> connectionLabel.setText(text));
    }

    public boolean isButtonPressed() {
        return lostConnection.isPressed();
    }
}
