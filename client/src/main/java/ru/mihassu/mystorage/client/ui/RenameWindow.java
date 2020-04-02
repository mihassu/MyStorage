package ru.mihassu.mystorage.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.mihassu.mystorage.common.ProvideDataCallback;

import java.io.IOException;

public class RenameWindow {

    private Stage stage;
    private RenameController controller;

    public RenameWindow(ProvideDataCallback provideNewFileName) throws IOException {
        this.stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/rename.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        controller = fxmlLoader.getController();
        controller.setProvideNewFileName(provideNewFileName);
        stage.setScene(scene);
    }

    public Stage setTitle(String title) {
        stage.setTitle(title);
//        controller.setRenameFile(title);
        return stage;
    }

//    public void show() {
//        stage.show();
//    }
}
