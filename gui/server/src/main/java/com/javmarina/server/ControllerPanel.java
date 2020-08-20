package com.javmarina.server;

import com.javmarina.util.Controller;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;


public class ControllerPanel extends JPanel {

    private final ButtonPanel a;
    private final ButtonPanel b;
    private final ButtonPanel x;
    private final ButtonPanel y;
    private final ButtonPanel l;
    private final ButtonPanel zl;
    private final ButtonPanel r;
    private final ButtonPanel zr;
    private final ButtonPanel plus;
    private final ButtonPanel minus;
    private final ButtonPanel home;
    private final ButtonPanel capture;
    private final DpadPanel dpad;
    private final JoystickPanel leftJoystick;
    private final JoystickPanel rightJoystick;

    public ControllerPanel() {
        super();

        setPreferredSize(new Dimension(500, 300));
        setLayout(null);

        a = new ButtonPanel("A", ButtonPanel.Shape.ROUND);
        a.setBounds(450, 135, 30, 30);

        b = new ButtonPanel("B", ButtonPanel.Shape.ROUND);
        b.setBounds(415, 170, 30, 30);

        x = new ButtonPanel("X", ButtonPanel.Shape.ROUND);
        x.setBounds(415, 100, 30, 30);

        y = new ButtonPanel("Y", ButtonPanel.Shape.ROUND);
        y.setBounds(380, 135, 30, 30);

        l = new ButtonPanel("L", ButtonPanel.Shape.RECTANGLE);
        l.setBounds(20, 60, 100, 30);

        zl = new ButtonPanel("ZL", ButtonPanel.Shape.SHOULDER);
        zl.setBounds(20, 20, 100, 30);

        r = new ButtonPanel("R", ButtonPanel.Shape.RECTANGLE);
        r.setBounds(380, 60, 100, 30);

        zr = new ButtonPanel("ZR", ButtonPanel.Shape.SHOULDER);
        zr.setBounds(380, 20, 100, 30);

        plus = new ButtonPanel("+", ButtonPanel.Shape.ROUND);
        plus.setBounds(340, 100, 30, 30);

        minus = new ButtonPanel("-", ButtonPanel.Shape.ROUND);
        minus.setBounds(130, 100, 30, 30);

        home = new ButtonPanel("⌂", ButtonPanel.Shape.ROUND);
        home.setBounds(300, 140, 30, 30);

        capture = new ButtonPanel("○", ButtonPanel.Shape.RECTANGLE);
        capture.setBounds(170, 140, 30, 30);

        dpad = new DpadPanel();
        dpad.setBounds(110, 210, 81, 81);

        leftJoystick = new JoystickPanel(100);
        leftJoystick.setBounds(20, 100, 100, 100);

        rightJoystick = new JoystickPanel(100);
        rightJoystick.setBounds(300, 200, 100, 100);

        add(a);
        add(b);
        add(x);
        add(y);
        add(l);
        add(zl);
        add(r);
        add(zr);
        add(dpad);
        add(plus);
        add(minus);
        add(home);
        add(capture);
        add(leftJoystick);
        add(rightJoystick);
    }

    public void updateUi(final byte[] packet) {
        assert packet.length == 8;

        final byte b0 = packet[0];
        plus.setPressed((b0 & (Controller.Button.PLUS >>> 8)) != 0);
        minus.setPressed((b0 & (Controller.Button.MINUS >>> 8)) != 0);
        home.setPressed((b0 & (Controller.Button.HOME >>> 8)) != 0);
        capture.setPressed((b0 & (Controller.Button.CAPTURE >>> 8)) != 0);
        leftJoystick.setPressed((b0 & (Controller.Button.LCLICK >>> 8)) != 0);
        rightJoystick.setPressed((b0 & (Controller.Button.RCLICK >>> 8)) != 0);

        final byte b1 = packet[1];
        a.setPressed((b1 & Controller.Button.A) != 0);
        b.setPressed((b1 & Controller.Button.B) != 0);
        x.setPressed((b1 & Controller.Button.X) != 0);
        y.setPressed((b1 & Controller.Button.Y) != 0);
        l.setPressed((b1 & Controller.Button.L) != 0);
        zl.setPressed((b1 & Controller.Button.ZL) != 0);
        r.setPressed((b1 & Controller.Button.R) != 0);
        zr.setPressed((b1 & Controller.Button.ZR) != 0);

        dpad.setState(packet[2]);

        leftJoystick.setJoystickPosition(packet[3], packet[4]);
        rightJoystick.setJoystickPosition(packet[5], packet[6]);
    }

    private static class ButtonPanel extends JPanel {

        private enum Shape {
            ROUND,
            RECTANGLE,
            SHOULDER
        }

        private static final Color COLOR_PRESSED = Color.GREEN;
        private static final Color COLOR_NOT_PRESSED = Color.LIGHT_GRAY;

        private static final int CORNER_RADIUS = 5;

        private final Shape shape;

        private Color backColour = COLOR_NOT_PRESSED;

        public ButtonPanel(final String text, final Shape shape) {
            super();

            this.shape = shape;

            setLayout(new GridBagLayout());
            final JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setOpaque(false);
            add(label);

            setPressed(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(30, 30);
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);

            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int h = g2.getClipBounds().height;
            final int w = g2.getClipBounds().width;
            switch (shape) {
                case ROUND:
                    g2.setColor(backColour);
                    g2.fillOval(0, 0, w, h);
                    break;
                case RECTANGLE:
                    g2.setColor(backColour);
                    final RoundRectangle2D roundedRectangle
                            = new RoundRectangle2D.Float(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS);
                    g2.fill(roundedRectangle);
                    break;
                case SHOULDER:
                    g2.setColor(backColour);
                    final RoundRectangle2D roundedRectangle2
                            = new RoundRectangle2D.Float(0, h/2.0f, w, h/2.0f, CORNER_RADIUS, CORNER_RADIUS);
                    g2.fill(roundedRectangle2);
                    g2.fillOval(0, 0, w, h);
                    break;
            }
        }

        private void setPressed(final boolean pressed) {
            backColour = pressed ? COLOR_PRESSED : COLOR_NOT_PRESSED;
            repaint();
        }
    }

    private static class DpadPanel extends JPanel {

        private static final Color COLOR_PRESSED = Color.DARK_GRAY;
        private static final Color COLOR_NOT_PRESSED = Color.LIGHT_GRAY;

        private int internalState;

        private DpadPanel() {
            setState(Controller.Dpad.CENTER);
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);

            final int h = g.getClipBounds().height;
            final int w = g.getClipBounds().width;

            final int[] x = {w/3, 2*w/3, w/3, 0};
            final int[] y = {0, h/3, 2*h/3, h/3};
            for (int i = 0; i < 4; i++) {
                g.setColor(((1 << i) & internalState) == 0 ? COLOR_NOT_PRESSED : COLOR_PRESSED);
                g.fillRect(x[i], y[i], w/3, h/3);
            }
            g.setColor(COLOR_NOT_PRESSED);
            g.fillRect(w/3, h/3, w/3, h/3);
        }

        private void setState(final byte state) {
            int internal = 0x00;
            if (state == Controller.Dpad.UP || state == Controller.Dpad.UP_RIGHT || state == Controller.Dpad.UP_LEFT) {
                internal |= 0x01;
            }
            if (state == Controller.Dpad.RIGHT || state == Controller.Dpad.UP_RIGHT || state == Controller.Dpad.DOWN_RIGHT) {
                internal |= 0x02;
            }
            if (state == Controller.Dpad.DOWN || state == Controller.Dpad.DOWN_LEFT || state == Controller.Dpad.DOWN_RIGHT) {
                internal |= 0x04;
            }
            if (state == Controller.Dpad.LEFT || state == Controller.Dpad.DOWN_LEFT || state == Controller.Dpad.UP_LEFT) {
                internal |= 0x08;
            }
            internalState = internal;
            repaint();
        }
    }

    private static class JoystickPanel extends JPanel {

        private static final int DEFAULT_DIAMETER = 100;
        private static final int STICK_DIAMETER = 40;
        private static final int THUMB_DIAMETER = 10;

        private static final Color COLOR_PRESSED = Color.GREEN;
        private static final Color COLOR_NOT_PRESSED = Color.DARK_GRAY;

        private final int diameter;
        
        /**
         * Joystick displayed Center, in pixels
         */
        private final int joyCenterX;
        private final int joyCenterY;

        private int dx;
        private int dy;
        private Color stickColor = COLOR_NOT_PRESSED;

        private JoystickPanel(final int diameter) {
            this.diameter = diameter <= 0 ? DEFAULT_DIAMETER : diameter;

            joyCenterX = diameter / 2;
            joyCenterY = diameter / 2;

            dx = (int) (diameter * (float) 128 / 255);
            dy = (int) (diameter - diameter * (float) 128 / 255);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(diameter, diameter);
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);

            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // background
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillOval(joyCenterX - diameter / 2, joyCenterY - diameter / 2, diameter, diameter);

            // center
            g2.setColor(Color.GRAY);
            g2.fillOval(joyCenterX - THUMB_DIAMETER / 2, joyCenterY - THUMB_DIAMETER / 2,
                    THUMB_DIAMETER, THUMB_DIAMETER);

            // stick
            g2.setColor(stickColor);
            g2.fillOval(dx - STICK_DIAMETER / 2, dy - STICK_DIAMETER / 2,
                    STICK_DIAMETER, STICK_DIAMETER);
        }

        @SuppressWarnings("NumericCastThatLosesPrecision")
        void setJoystickPosition(final int x, final int y) {
            // x and y come in binary format. Use inverted two's complement
            int x2 = x;
            if (x2 < 0) {
                x2 += 256;
            }
            int y2 = y;
            if (y2 < 0) {
                y2 += 256;
            }
            dx = (int) (diameter * (float) x2 / 255);
            dy = (int) (diameter - diameter * (float) y2 / 255);
            repaint();
        }

        private void setPressed(final boolean pressed) {
            stickColor = pressed ? COLOR_PRESSED : COLOR_NOT_PRESSED;
            repaint();
        }
    }
}
