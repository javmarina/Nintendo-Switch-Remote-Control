package com.javmarina.client;

import com.javmarina.client.services.ControllerService;
import com.javmarina.client.services.KeyboardService;
import com.javmarina.util.StoppableLoop;
import com.javmarina.webrtc.FramerateEstimator;
import com.javmarina.webrtc.RtcClient;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.ResourceBundle;


public class ConnectionFrame implements RtcClient.Callback {

    static final ResourceBundle RESOURCE_BUNDLE =
            ResourceBundle.getBundle("connection", Locale.getDefault());

    private final RtcClient rtcClient;
    private final ControllerService service;
    private final Callback callback;

    private Timeline timeline;
    private Thread thread;
    private FrameProcessing frameProcessing;
    private ConnectionController connectionController;
    private Stage stage;

    public ConnectionFrame(final ControllerService service,
                           final SessionId sessionId,
                           final AudioDevice audioDevice,
                           final Callback callback) {
        this.service = service;
        this.callback = callback;

        Client.deviceModule.setPlayoutDevice(audioDevice);
        // TODO: this doesn't currently work, defaults to speaker

        this.rtcClient = new RtcClient(
                sessionId,
                service::getControllerStatus,
                Client.deviceModule,
                this
        );
    }

    public void show() throws IOException {
        final FXMLLoader loader = new FXMLLoader(
                ConnectionFrame.class.getResource("/view/connection.fxml"), RESOURCE_BUNDLE);
        final HBox page = loader.load();
        final Scene scene = new Scene(page);

        connectionController = loader.getController();
        connectionController.setButtonListener(() -> {
            System.out.println(RESOURCE_BUNDLE.getString("connection.stoppingSession"));
            rtcClient.stop();
        });
        connectionController.setButtonEnabled(false);

        if (service instanceof KeyboardService) {
            ((KeyboardService) service).setScene(scene);
        }

        frameProcessing = new FrameProcessing(new FrameProcessing.Callback() {
            @Override
            public void onImage(final Image image) {
                connectionController.newImage(image);
            }

            @Override
            public void onFramerate(final double framerate) {
                Platform.runLater(() -> connectionController.setFramerateValue(framerate));
            }
        });
        thread = new Thread(frameProcessing);

        timeline = new Timeline(new KeyFrame(
                Duration.seconds(1),
                event -> rtcClient.getStats(connectionController::newStats)
        ));
        timeline.setCycleCount(Animation.INDEFINITE);

        try {
            rtcClient.start(); // onSessionStarted() will be called if successful
        } catch (final Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(RESOURCE_BUNDLE.getString("connection.error"));
                alert.setHeaderText(null);
                alert.showAndWait();

                close();
            });
        }

        stage = new Stage();
        stage.setTitle(RESOURCE_BUNDLE.getString("connection.title"));
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void onRttReplyReceived(final int milliseconds) {
        connectionController.newRtt(milliseconds);
    }

    @Override
    public void onSessionStarted() {
        service.start();
        thread.start();
        timeline.play();
        connectionController.setButtonEnabled(true);
    }

    @Override
    public void onSessionStopped() {
        timeline.stop();
        service.finish();
        frameProcessing.stop(() -> Platform.runLater(this::close));
    }

    @Override
    public void onInvalidSessionId() {
        Platform.runLater(() -> {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(RESOURCE_BUNDLE.getString("connection.invalidSessionID"));
            alert.setHeaderText(null);
            alert.showAndWait();

            close();
        });
    }

    @Override
    public void onVideoFrame(final VideoFrame frame) {
        frameProcessing.newFrame(frame);
    }

    private void close() {
        stage.close();
        callback.onClosed();
    }

    private static final class FrameProcessing extends StoppableLoop {

        private final Object lock = new Object();
        private final Callback callback;
        private final FramerateEstimator framerateEstimator;

        @Nullable
        private VideoFrame nextFrame = null;
        private PixelBuffer<ByteBuffer> pixelBuffer;
        private ByteBuffer byteBuffer;

        private FrameProcessing(final Callback callback) {
            this.callback = callback;
            this.framerateEstimator = new FramerateEstimator(callback::onFramerate);
        }

        @Override
        public void loop() {
            final VideoFrame currentFrame;
            synchronized (lock) {
                currentFrame = nextFrame;
                nextFrame = null;
            }
            if (currentFrame != null) {
                final VideoFrameBuffer buffer = currentFrame.buffer;
                final int width = buffer.getWidth();
                final int height = buffer.getHeight();

                if (pixelBuffer == null || pixelBuffer.getWidth() != width || pixelBuffer.getHeight() != height) {
                    byteBuffer = ByteBuffer.allocate(width * height * 4);
                    pixelBuffer = new PixelBuffer<>(width, height, byteBuffer, PixelFormat.getByteBgraPreInstance());

                    callback.onImage(new WritableImage(pixelBuffer));
                }

                try {
                    VideoBufferConverter.convertFromI420(buffer, byteBuffer, FourCC.ARGB);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                Platform.runLater(() -> pixelBuffer.updateBuffer(pixBuffer -> null));

                framerateEstimator.onVideoFrame(currentFrame);

                currentFrame.release();
            }
        }

        private void newFrame(final VideoFrame frame) {
            synchronized (lock) {
                if (nextFrame != null) {
                    nextFrame.release();
                }
                frame.retain();
                nextFrame = frame;
            }
        }

        private interface Callback {
            void onImage(final Image image);
            void onFramerate(final double framerate);
        }
    }

    public interface Callback {
        void onClosed();
    }
}
