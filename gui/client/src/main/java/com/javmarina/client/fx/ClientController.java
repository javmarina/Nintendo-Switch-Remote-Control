package com.javmarina.client.fx;

import com.javmarina.client.services.ControllerService;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


public class ClientController {

    private static final Pattern COMPILE = Pattern.compile("^[0-9A-F]+$");

    @FXML
    private TextField sessionIdField;
    @FXML
    private ChoiceBox<ControllerService> controllerInput;
    @FXML
    private ChoiceBox<AudioDevice> audioOutput;
    @FXML
    private Button startButton;

    @FXML
    private void initialize() {
        startButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !SessionId.validateString(sessionIdField.getText())
                        || controllerInput.getValue() == null
                        || audioOutput.getValue() == null,
                sessionIdField.textProperty(), controllerInput.valueProperty(), audioOutput.valueProperty()));

        // force the field to be hex only
        sessionIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!COMPILE.matcher(newValue.toUpperCase(Locale.ROOT)).matches() || newValue.length() > 4) {
                sessionIdField.setText(oldValue);
            }
        });

        audioOutput.setConverter(new StringConverter<>() {
            @Override
            public String toString(final AudioDevice audioDevice) {
                return audioDevice.getName();
            }

            @Override
            public AudioDevice fromString(final String string) {
                return audioOutput.getItems().stream()
                        .filter(audioDevice -> audioDevice.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    public void setControllerServices(final List<ControllerService> controllerServices) {
        controllerInput.setItems(FXCollections.observableList(controllerServices));
        controllerInput.getSelectionModel().selectFirst();
    }

    public void setAudioOutputDevices(final List<AudioDevice> audioDevices) {
        audioOutput.setItems(FXCollections.observableList(audioDevices));
        audioOutput.getSelectionModel().selectFirst();
    }

    public void setButtonAction(final Runnable runnable) {
        startButton.setOnAction(event -> runnable.run());
    }

    public ControllerService getSelectedControllerService() {
        return controllerInput.getValue();
    }

    public AudioDevice getSelectedAudioDevice() {
        return audioOutput.getValue();
    }

    public SessionId getSessionId() {
        return SessionId.fromString(sessionIdField.getText());
    }
}
