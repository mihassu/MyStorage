package ru.mihassu.mystorage.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import ru.mihassu.mystorage.common.Constants;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController implements Initializable {

    private static Logger logger = Logger.getLogger(MainController.class.getName());

    @FXML
    TextField fileNameField;

    @FXML
    ListView<String> clientFilesList;

    @FXML
    ListView<String> serverFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.getInstance().setCallOnListRefresh((filesNames) -> {
            refreshClientList(clientFilesList, Constants.clientDir);
            refreshServerList(filesNames);
            logIt("Callback - refresh");
        });

        final CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        try {
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //обновить списки на сервере и на клиенте
        Network.getInstance().getServerFiles();

        initItemsSelectedListeners();
    }



    private void refreshServerList(List<String> filesNames) {
        Platform.runLater(() -> {
            serverFilesList.getItems().clear();
            for (String f: filesNames) {
                serverFilesList.getItems().add(f);
            }
        });
    }

    private void refreshClientList(ListView<String> filesList, String dir) {
        Platform.runLater(() -> {
            try {
                filesList.getItems().clear();
                Files
                        .list(Paths.get(dir)) //List<Path>
                        .map(path -> path.getFileName().toString()) //List<String>
                        .forEach(fileName -> filesList.getItems().add(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void onPressUploadBtn(ActionEvent actionEvent) {
        if (fileNameField.getLength() > 0) {
            Network.getInstance().sendFile(Paths.get(Constants.clientDir + fileNameField.getText()));
            fileNameField.clear();
        }
    }

    public void onPressDownloadBtn(ActionEvent actionEvent) {
        Network.getInstance().downloadFile(fileNameField.getText());
    }

    public void onPressDeletedBtn(ActionEvent actionEvent) {
        try {
            Files.delete(Paths.get(Constants.clientDir + fileNameField.getText()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshClientList(clientFilesList, Constants.clientDir);

    }

    public void onPressDeletedServerBtn(ActionEvent actionEvent) {
        Network.getInstance().deleteServerFile(fileNameField.getText());
    }

    private void initItemsSelectedListeners() {
        MultipleSelectionModel<String> clientSelectionModel = clientFilesList.getSelectionModel();
        clientSelectionModel
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> fileNameField.setText(newValue));

        MultipleSelectionModel<String> serverSelectionModel = serverFilesList.getSelectionModel();
        serverSelectionModel
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> fileNameField.setText(newValue));
    }

    private void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private static void logIt(String logText) {
        logger.log(Level.SEVERE, logText);
    }



}
