package com.javmarina.server.fx;

import com.fazecast.jSerialComm.SerialPort;
import com.javmarina.server.SerialAdapter;
import com.javmarina.webrtc.WebRtcLoader;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class ServerFx extends Application {

    private static List<AudioDevice> AUDIO_DEVICES = new ArrayList<>(0);
    public static AudioDeviceModule deviceModule;

    static {
        WebRtcLoader.loadLibrary();
    }

    public static void main(final String[] args) {
        try {
            AUDIO_DEVICES = MediaDevices.getAudioCaptureDevices();
            deviceModule = new AudioDeviceModule();
        } catch (final Exception ignored) {
        }
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final FXMLLoader loader = new FXMLLoader(
                ServerFx.class.getResource("/view/server.fxml"));
        final GridPane page = loader.load();
        final Scene scene = new Scene(page);

        final ServerController serverController = loader.getController();

        final List<SerialPort> ports = new ArrayList<>(Arrays.asList(SerialPort.getCommPorts()));
        ports.add(0, null);
        serverController.setSerialPorts(ports);
        serverController.setVideoInputDevices(MediaDevices.getVideoCaptureDevices());
        serverController.setAudioInputDevices(AUDIO_DEVICES);
        serverController.setButtonAction(() -> {
            final SerialPort serialPort = serverController.getSelectedSerialPort();
            serverController.stopVideoPreview();

            final ConnectionFrame connectionFrame = new ConnectionFrame(
                    new SerialAdapter(serialPort, serverController.getBaudrate()),
                    serverController.getSessionId(),
                    serverController.getVideoDeviceSource(),
                    serverController.getSelectedAudioDevice(),
                    () -> {
                        primaryStage.show();
                        serverController.reload();
                    }
            );
            try {
                connectionFrame.show();
                primaryStage.hide();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });

        primaryStage.setTitle("Server configuration");
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getWidth()/2);
        primaryStage.setY((bounds.getHeight() - primaryStage.getHeight()) / 2);
    }
}
