package com.javmarina.server;

import com.fazecast.jSerialComm.SerialPort;
import com.javmarina.webrtc.RtcUtils;
import com.javmarina.webrtc.WebRtcLoader;
import dev.onvoid.webrtc.media.MediaDevices;
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
import java.util.Locale;
import java.util.ResourceBundle;


public final class Server extends Application {

    static final ResourceBundle RESOURCE_BUNDLE =
            ResourceBundle.getBundle("server", Locale.getDefault());

    private ServerController serverController;

    public static void main(final String[] args) {
        WebRtcLoader.loadLibrary();
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final FXMLLoader loader = new FXMLLoader(
                Server.class.getResource("/view/server.fxml"), RESOURCE_BUNDLE);
        final GridPane page = loader.load();
        final Scene scene = new Scene(page);

        serverController = loader.getController();

        final List<SerialPort> ports = new ArrayList<>(Arrays.asList(SerialPort.getCommPorts()));
        ports.add(0, null); // Add "None" option
        serverController.setSerialPorts(ports);
        serverController.setVideoInputDevices(MediaDevices.getVideoCaptureDevices());
        serverController.setAudioInputDevices(RtcUtils.getAudioCaptureDevicesBlocking());
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

        primaryStage.setTitle(RESOURCE_BUNDLE.getString("server.title"));
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getWidth()/2);
        primaryStage.setY((bounds.getHeight() - primaryStage.getHeight()) / 2);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        serverController.stop();
        // Thread-5 prevents the app from closing gracefully
        System.exit(0);
    }
}
