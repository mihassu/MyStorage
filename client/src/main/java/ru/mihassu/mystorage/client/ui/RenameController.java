package ru.mihassu.mystorage.client.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import ru.mihassu.mystorage.common.ProvideDataCallback;

public class RenameController {

    private ProvideDataCallback newFileNameCallback;

    @FXML
    TextField newFileNameField;


    public void onPressRenameBtn(ActionEvent actionEvent) {
        newFileNameCallback.provide(newFileNameField.getText());
        newFileNameField.getScene().getWindow().hide();
    }

    public void onPressCancelBtn(ActionEvent actionEvent) {
        newFileNameField.getScene().getWindow().hide();
    }

    public void setNewFileNameCallback(ProvideDataCallback newFileNameCallback) {
        this.newFileNameCallback = newFileNameCallback;
    }
}
