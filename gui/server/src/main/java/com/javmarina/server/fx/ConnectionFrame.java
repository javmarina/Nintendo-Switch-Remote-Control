package com.javmarina.server.fx;

import com.javmarina.server.SerialAdapter;
import com.javmarina.util.GeneralUtils;
import com.javmarina.util.Packet;
import com.javmarina.webrtc.RtcServer;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;


public class ConnectionFrame implements RtcServer.Callback {

    private final RtcServer rtcServer;
    private final SerialAdapter serialAdapter;
    private final SessionId sessionId;
    private final Callback callback;

    private PanelController panelController;
    private Stage stage;

    public ConnectionFrame(final SerialAdapter serialAdapter, final SessionId sessionId,
                           final VideoDeviceSource videoDeviceSource,
                           final AudioDevice audioDevice,
                           final Callback callback) {
        // TODO: this doesn't seem to work
        ServerFx.deviceModule.setRecordingDevice(audioDevice);

        rtcServer = new RtcServer(
                sessionId,
                ServerFx.deviceModule,
                videoDeviceSource,
                this
        );

        this.serialAdapter = serialAdapter;
        this.sessionId = sessionId;
        this.callback = callback;
    }

    public void show() throws IOException {
        final FXMLLoader loader = new FXMLLoader(
                ConnectionFrame.class.getResource("/view/connection.fxml"));
        final AnchorPane page = loader.load();
        final Scene scene = new Scene(page);

        panelController = loader.getController();

        try {
            panelController.setSerialInfo("Trying to sync");
            serialAdapter.sync(true);
            panelController.setSerialInfo("Synced with serial adapter");
            System.out.println("Synced with serial adapter");
        } catch (final IOException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(e.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();

            serialAdapter.closePort();
            close();
            return;
        }

        if (!serialAdapter.isFake()) {
            runSerialPortTests(serialAdapter);
        }

        new Thread(() -> {
            try {
                rtcServer.start();
            } catch (final Exception e) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("An error occurred, please try again later");
                alert.setHeaderText(null);
                alert.showAndWait();

                serialAdapter.closePort();
                close();
            }
        }).start();

        stage = new Stage();
        stage.setTitle(String.format("Server (session ID: %s)", sessionId.toString()));
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getWidth()/2);
        stage.setY((bounds.getHeight() - stage.getHeight()) / 2);
    }

    @Override
    public void onPacketReceived(final Packet packet) {
        final String message = GeneralUtils.byteArrayToString(packet.getBuffer());
        panelController.setConnectionInfo("From client: " + message);
        // Update UI
        panelController.updateUi(packet);
        // Send to MCU
        if (!panelController.isButtonPressed()) {
            final boolean result = serialAdapter.sendPacket(packet);
            panelController.setSerialInfo(result ? "Synced with serial adapter" : "Serial packet error");
        }
    }

    @Override
    public void onSessionStarted() {
    }

    @Override
    public void onSessionStopped() {
        Platform.runLater(() -> {
            System.out.println("Session stopped");
            serialAdapter.closePort();
            close();
        });
    }

    @Override
    public void onError(final Exception e) {
        Platform.runLater(() -> {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(e.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();

            serialAdapter.closePort();
            close();
        });
    }

    @Override
    public void onInvalidSessionId() {
        Platform.runLater(() -> {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Invalid session ID");
            alert.setHeaderText(null);
            alert.showAndWait();

            serialAdapter.closePort();
            close();
        });
    }

    private static void runSerialPortTests(final SerialAdapter serialAdapter) {
        final SerialAdapter.TestResults testResults = serialAdapter.testSpeed(100);
        final String msg;
        String url = null;
        switch (testResults.errorType) {
            case NONE:
                final String temp = String.format("Minimum: %d ms\r\nMaximum: %d ms\r\nAverage: %.3f ms\r\nError count: %d",
                        testResults.min, testResults.max, testResults.avg, testResults.errorCount);
                if (testResults.avg > 10.0) {
                    url = "https://projectgus.com/2011/10/notes-on-ftdi-latency-with-arduino/";
                    msg = temp + "\r\nAverage is high. You might need to adjust the latency timer of the FTDI adapter." +
                            "\r\nSee " + url + " for more info.";
                } else {
                    msg = temp;
                }
                break;
            case NO_ACKS:
                msg = "No packets were sent correctly";
                break;
            case SYNC_ERROR:
                msg = "Sync error, test aborted";
                break;
            default:
                msg = "Unknown error";
                break;
        }
        if (url != null) {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText(msg);
            alert.setHeaderText(null);

            final ButtonType buttonOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            final ButtonType buttonOpen = new ButtonType("Open");
            alert.getButtonTypes().setAll(buttonOk, buttonOpen);

            final Optional<ButtonType> result = alert.showAndWait();
            if (result.orElse(null) == buttonOpen) {
                try {
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (final IOException ignored) {
                }
            }
        } else {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText(msg);
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private void close() {
        stage.close();
        callback.onClosed();
    }

    public interface Callback {
        void onClosed();
    }
}
