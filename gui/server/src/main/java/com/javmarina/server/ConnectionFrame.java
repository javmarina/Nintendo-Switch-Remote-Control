package com.javmarina.server;

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
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;


public class ConnectionFrame implements RtcServer.Callback {

    private static final ResourceBundle RESOURCE_BUNDLE =
            ResourceBundle.getBundle("connection", Locale.getDefault());

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
        Server.deviceModule.setRecordingDevice(audioDevice);

        rtcServer = new RtcServer(
                sessionId,
                Server.deviceModule,
                videoDeviceSource,
                this
        );

        this.serialAdapter = serialAdapter;
        this.sessionId = sessionId;
        this.callback = callback;
    }

    public void show() throws IOException {
        final FXMLLoader loader = new FXMLLoader(
                ConnectionFrame.class.getResource("/view/connection.fxml"), RESOURCE_BUNDLE);
        final AnchorPane page = loader.load();
        final Scene scene = new Scene(page);

        panelController = loader.getController();

        try {
            panelController.setSerialInfo(RESOURCE_BUNDLE.getString("connection.serialSyncing"));
            serialAdapter.sync(true);
            panelController.setSerialInfo(RESOURCE_BUNDLE.getString("connection.serialSynced"));
            System.out.println(RESOURCE_BUNDLE.getString("connection.serialSynced"));
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
                alert.setContentText(RESOURCE_BUNDLE.getString("connection.error"));
                alert.setHeaderText(null);
                alert.showAndWait();

                serialAdapter.closePort();
                close();
            }
        }).start();

        stage = new Stage();
        stage.setTitle(String.format(RESOURCE_BUNDLE.getString("connection.title"), sessionId.toString()));
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
        panelController.setConnectionInfo(String.format(RESOURCE_BUNDLE.getString("connection.fromClient"), message));
        // Update UI
        panelController.updateUi(packet);
        // Send to MCU
        if (!panelController.isButtonPressed()) {
            final boolean result = serialAdapter.sendPacket(packet);
            panelController.setSerialInfo(
                    RESOURCE_BUNDLE.getString(result ? "connection.serialSynced" : "connection.serialError")
            );
        }
    }

    @Override
    public void onSessionStarted() {
    }

    @Override
    public void onSessionStopped() {
        Platform.runLater(() -> {
            System.out.println(RESOURCE_BUNDLE.getString("connection.sessionStopped"));
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
            alert.setContentText(RESOURCE_BUNDLE.getString("connection.invalidSessionID"));
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
                final String temp = String.format(RESOURCE_BUNDLE.getString("connection.serialTestResult"),
                        testResults.min, testResults.max, testResults.avg, testResults.errorCount);
                if (testResults.avg > 10.0) {
                    url = "https://projectgus.com/2011/10/notes-on-ftdi-latency-with-arduino/";
                    msg = temp + String.format(RESOURCE_BUNDLE.getString("connection.serialTestResultExtra"), url);
                } else {
                    msg = temp;
                }
                break;
            case NO_ACKS:
                msg = RESOURCE_BUNDLE.getString("connection.serialTestNoAcks");
                break;
            case SYNC_ERROR:
                msg = RESOURCE_BUNDLE.getString("connection.serialTestSyncError");
                break;
            default:
                msg = RESOURCE_BUNDLE.getString("connection.serialTestUnknownError");
                break;
        }
        if (url != null) {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText(msg);
            alert.setHeaderText(null);

            final ButtonType buttonOk = new ButtonType(
                    RESOURCE_BUNDLE.getString("connection.buttonOk"),
                    ButtonBar.ButtonData.OK_DONE
            );
            final ButtonType buttonOpen = new ButtonType(RESOURCE_BUNDLE.getString("connection.buttonOpen"));
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
