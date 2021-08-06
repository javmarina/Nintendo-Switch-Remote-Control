package com.javmarina.server;

import com.fazecast.jSerialComm.SerialPort;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;
import dev.onvoid.webrtc.media.video.VideoTrack;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.util.StringConverter;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ServerController {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[0-9]*");

    private static final int DEFAULT_BAUDRATE = 1000000; // 1 Mbps
    private static final String KEY_BAUDRATE = "key_baudrate";

    private static final Comparator<VideoCaptureCapability> CAPABILITY_COMPARATOR = (o1, o2) -> {
        final int compare1 = Integer.compare(
                o1.width*o1.height,
                o2.width*o2.height
        );
        if (compare1 == 0) {
            return Integer.compare(o1.frameRate, o2.frameRate);
        } else {
            return compare1;
        }
    };

    @FXML
    private Label sessionIdField;
    @FXML
    private TextField baudrateField;
    @FXML
    private ChoiceBox<SerialPort> serialPort;
    @FXML
    private ChoiceBox<VideoDevice> videoInput;
    @FXML
    private ChoiceBox<VideoCaptureCapability> videoCapability;
    @FXML
    private ChoiceBox<AudioDevice> audioInput;
    @FXML
    private Button openServerButton;
    @FXML
    private ImageView videoPreview;
    private PixelBuffer<ByteBuffer> pixelBuffer;
    private ByteBuffer byteBuffer;

    private final PeerConnectionFactory factory = new PeerConnectionFactory();
    private VideoDevice currentVideoDevice;
    private final VideoDeviceSource videoDeviceSource = new VideoDeviceSource();
    private VideoCaptureCapability currentVideoCapability;
    private VideoTrack videoTrack;

    private final Preferences prefs = Preferences.userNodeForPackage(ServerController.class);

    @FXML
    private void initialize() {
        final SessionId sessionId = new SessionId();
        sessionIdField.setText(sessionId.toString());

        baudrateField.setText(prefs.get(KEY_BAUDRATE, String.valueOf(DEFAULT_BAUDRATE)));

        openServerButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            final int baud = baudrateField.getText().length() == 0 ?
                    0 : Integer.parseInt(baudrateField.getText());
            return baud < 9600 || baud > 1000000
                    || videoInput.getValue() == null
                    || videoCapability.getValue() == null
                    || audioInput.getValue() == null;
        }, baudrateField.textProperty(), videoInput.valueProperty(),
                videoCapability.valueProperty(), audioInput.valueProperty()));

        // force the field to be numeric only
        final UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            final String input = change.getText();
            if (NUMERIC_PATTERN.matcher(input).matches()) {
                return change;
            }
            return null;
        };
        baudrateField.setTextFormatter(new TextFormatter<String>(integerFilter));
        baudrateField.textProperty().addListener((observable, oldValue, newValue) -> prefs.put(KEY_BAUDRATE, newValue));
        
        serialPort.setConverter(new StringConverter<>() {
            
            private final String NULL_NAME = Server.RESOURCE_BUNDLE.getString("server.portNone");
            
            @Override
            public String toString(final SerialPort serialPort) {
                return serialPort != null ? serialPort.getDescriptivePortName() : NULL_NAME;
            }

            @Override
            public SerialPort fromString(final String string) {
                if (NULL_NAME.equals(string)) {
                    return null;
                } else {
                    return serialPort.getItems().stream()
                            .filter(serialPort -> serialPort.getDescriptivePortName().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            }
        });

        videoInput.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            currentVideoDevice = newValue;
            videoDeviceSource.setVideoCaptureDevice(currentVideoDevice);

            final List<VideoCaptureCapability> capabilities =
                    MediaDevices.getVideoCaptureCapabilities(currentVideoDevice).stream()
                            .distinct()
                            .sorted(CAPABILITY_COMPARATOR)
                            .collect(Collectors.toList());
            videoCapability.setItems(FXCollections.observableList(capabilities));
        });
        videoInput.setConverter(new StringConverter<>() {
            @Override
            public String toString(final VideoDevice videoDevice) {
                return videoDevice.getName();
            }

            @Override
            public VideoDevice fromString(final String string) {
                return videoInput.getItems().stream()
                        .filter(videoDevice -> videoDevice.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        videoCapability.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            currentVideoCapability = newValue;

            if (videoTrack != null) {
                videoTrack.dispose();
            }
            videoDeviceSource.stop();
            videoDeviceSource.setVideoCaptureDevice(currentVideoDevice);
            videoDeviceSource.setVideoCaptureCapability(currentVideoCapability);

            videoTrack = factory.createVideoTrack("videoTrack", videoDeviceSource);
            videoTrack.addSink(frame -> {
                frame.retain();
                final VideoFrameBuffer buffer = frame.buffer;
                final int width = buffer.getWidth();
                final int height = buffer.getHeight();

                if (pixelBuffer == null || pixelBuffer.getWidth() != width || pixelBuffer.getHeight() != height) {
                    byteBuffer = ByteBuffer.allocate(width * height * 4);
                    pixelBuffer = new PixelBuffer<>(width, height, byteBuffer, PixelFormat.getByteBgraPreInstance());

                    videoPreview.setImage(new WritableImage(pixelBuffer));
                }

                try {
                    VideoBufferConverter.convertFromI420(buffer, byteBuffer, FourCC.ARGB);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                Platform.runLater(() -> pixelBuffer.updateBuffer(pixBuffer -> null));

                frame.release();
            });
            videoDeviceSource.start();
        });
        videoCapability.setConverter(new StringConverter<>() {
            @Override
            public String toString(final VideoCaptureCapability capability) {
                return String.format("%dx%d@%d", capability.width, capability.height, capability.frameRate);
            }

            @Override
            public VideoCaptureCapability fromString(final String string) {
                final String[] parts = string.split("@");
                final String[] parts2 = parts[0].split("x");
                final int width = Integer.parseInt(parts2[0]);
                final int height = Integer.parseInt(parts2[1]);
                final int framerate = Integer.parseInt(parts[1]);
                return new VideoCaptureCapability(width, height, framerate);
            }
        });

        audioInput.setConverter(new StringConverter<>() {
            @Override
            public String toString(final AudioDevice audioDevice) {
                return audioDevice.getName();
            }

            @Override
            public AudioDevice fromString(final String string) {
                return audioInput.getItems().stream()
                        .filter(audioDevice -> audioDevice.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    public void setSerialPorts(final List<SerialPort> serialPorts) {
        serialPort.setItems(FXCollections.observableList(serialPorts));
        serialPort.getSelectionModel().selectFirst();
    }

    public void setVideoInputDevices(final List<VideoDevice> videoDevices) {
        videoInput.setItems(FXCollections.observableList(videoDevices));
        videoInput.getSelectionModel().selectFirst();
    }

    public void setAudioInputDevices(final List<AudioDevice> audioDevices) {
        audioInput.setItems(FXCollections.observableList(audioDevices));
        audioInput.getSelectionModel().selectFirst();
    }

    public void setButtonAction(final Runnable runnable) {
        openServerButton.setOnAction(event -> runnable.run());
    }

    public SessionId getSessionId() {
        return SessionId.fromString(sessionIdField.getText());
    }

    public int getBaudrate() {
        return Integer.parseInt(baudrateField.getText());
    }

    public SerialPort getSelectedSerialPort() {
        return serialPort.getValue();
    }

    public VideoDeviceSource getVideoDeviceSource() {
        return videoDeviceSource;
    }

    public AudioDevice getSelectedAudioDevice() {
        return audioInput.getValue();
    }

    public void stopVideoPreview() {
        videoDeviceSource.stop();
    }

    public void reload() {
        videoDeviceSource.start();
        final SessionId sessionId = new SessionId();
        sessionIdField.setText(sessionId.toString());
    }
}
