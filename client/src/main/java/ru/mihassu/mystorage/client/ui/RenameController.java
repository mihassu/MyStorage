package ru.mihassu.mystorage.client.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import ru.mihassu.mystorage.common.ProvideDataCallback;

public class RenameController {

//    private String renameFile;
    private ProvideDataCallback provideNewFileName;

    @FXML
    TextField newFileNameField;


    public void onPressRenameBtn(ActionEvent actionEvent) {
        provideNewFileName.provide(newFileNameField.getText());
        newFileNameField.getScene().getWindow().hide();
    }

    public void onPressCancelBtn(ActionEvent actionEvent) {
        newFileNameField.getScene().getWindow().hide();
    }

//    public void setRenameFile(String renameFile) {
//        this.renameFile = renameFile;
//    }

    public void setProvideNewFileName(ProvideDataCallback provideNewFileName) {
        this.provideNewFileName = provideNewFileName;
    }
}
