package com.javmarina.client;

import com.javmarina.client.services.ControllerService;
import com.javmarina.webrtc.SdpUtils;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.Comparator;
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
    private ChoiceBox<SdpUtils.CodecPreference> codecPreference;
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

        codecPreference.setItems(FXCollections.observableArrayList(SdpUtils.CodecPreference.getAvailablePreferences()));
        codecPreference.setValue(SdpUtils.CodecPreference.VP9);

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

    @FXML
    public void onEnter(final ActionEvent ae){
        if (!startButton.isDisabled()) {
            startButton.getOnAction().handle(ae);
        }
    }

    public void setControllerServices(final List<ControllerService> controllerServices) {
        final ObservableList<ControllerService> observableList = FXCollections.observableList(controllerServices);
        FXCollections.sort(observableList, Comparator.comparing(ControllerService::toString));
        controllerInput.setItems(observableList);
        controllerInput.getSelectionModel().selectFirst();
    }

    public void setAudioOutputDevices(final List<AudioDevice> audioDevices) {
        final ObservableList<AudioDevice> observableList = FXCollections.observableList(audioDevices);
        FXCollections.sort(observableList, Comparator.comparing(AudioDevice::getName));
        audioOutput.setItems(observableList);
        audioOutput.getSelectionModel().selectFirst();
    }

    public void setButtonAction(final Runnable runnable) {
        startButton.setOnAction(event -> runnable.run());
    }

    public ControllerService getSelectedControllerService() {
        return controllerInput.getValue();
    }

    public SdpUtils.CodecPreference getPreferredVideoCodec() {
        return codecPreference.getValue();
    }

    public AudioDevice getSelectedAudioDevice() {
        return audioOutput.getValue();
    }

    public SessionId getSessionId() {
        return SessionId.fromString(sessionIdField.getText());
    }
}
