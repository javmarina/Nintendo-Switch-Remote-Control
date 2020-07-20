package com.javmarina.client;

import com.javmarina.util.GeneralUtils;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;


public class DelayGraphPanel extends JPanel {

    private static final int MAX_ITEMS = 50;
    private static final float BETA = 0.25f; // Filter constant

    private final ArrayList<Integer> delays = new ArrayList<>(MAX_ITEMS);
    private final ArrayList<Float> estimated = new ArrayList<>(MAX_ITEMS);

    @Override
    public Dimension getPreferredSize() {
        // final int max = delays.stream().reduce(-1, Integer::max);
        return new Dimension(delays.size(), 260);
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        final int length = delays.size();
        if (length == 0) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background
        g2.setBackground(Color.LIGHT_GRAY);

        // Draw horizontal axis
        g2.setColor(Color.BLACK);
        final int yZero = getHeight() - 10;
        g2.drawLine(0, yZero, getWidth(), yZero);

        double sum = 0;
        int min = Integer.MAX_VALUE;
        int max = -1;
        for (final int delay : delays) {
            sum += delay;
            if (delay > max) {
                max = delay;
            }
            if (delay < min) {
                min = delay;
            }
        }
        max = max > 0 ? max : 1;

        g2.setColor(Color.GRAY);
        final int yAverage = getHeight() - 10 - (int) ((160.0/max) * sum/length);
        g2.drawLine(0, yAverage, getWidth(), yAverage);

        final int[] x = new int[length];
        final int[] y = new int[length];
        final int[] yEstimated = new int[length];
        for (int i = 0; i < length; i++) {
            x[i] = (int) Math.ceil(getWidth()*(i+1.0)/(MAX_ITEMS+2));
            y[i] = getHeight() - 10 - (int) (160.0/max) * delays.get(i);
            yEstimated[i] = getHeight() - 10 - (int) ((160.0/max) * estimated.get(i));
        }

        g2.setColor(Color.RED);
        g2.drawPolyline(x, yEstimated, length);

        g2.setColor(Color.BLUE);
        g2.drawPolyline(x, y, length);
        if (length < MAX_ITEMS) {
            final Ellipse2D.Double circle = new Ellipse2D.Double(x[length-1]-1, y[length-1]-1, 4, 4);
            g2.fill(circle);
        }

        String average = String.valueOf(sum/length);
        if (average.length() > 6) {
            average = average.substring(0, 6);
        }

        g2.setColor(Color.BLACK);
        g2.drawString("Average RTT: " + average + " ms", 5, 15);
        g2.drawString("Minimum RTT: " + min + " ms", 5, 30);
        g2.drawString("Maximum RTT: " + max + " ms", 5, 45);
    }

    public void addDelay(final int delay) {
        // First remove, then add
        if (delays.size() == MAX_ITEMS) {
            delays.remove(0);
            estimated.remove(0);
        }
        delays.add(delay);
        if (estimated.isEmpty()) {
            estimated.add((float) delay);
        } else {
            estimated.add(GeneralUtils.lowPassFilter(estimated.get(estimated.size()-1), delay, BETA));
        }
        repaint();
    }
}
