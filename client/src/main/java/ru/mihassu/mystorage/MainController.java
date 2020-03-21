package ru.mihassu.mystorage;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;

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
    TextField taFileName;

    @FXML
    ListView<String> clientFilesList;

    @FXML
    ListView<String> serverFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.getInstance().setCallOnFileSent((filesNames) -> {
            refreshClientList(clientFilesList, "client-storage");
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
        refreshClientList(clientFilesList, "client-storage");

        MultipleSelectionModel<String> clientSelectionModel = clientFilesList.getSelectionModel();
        clientSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                taFileName.setText(newValue);
            }
        });



//        Thread read = new Thread(() -> {
//            try {
//                while (true) {
//                    int i = NetworkIO.getInstance().readServerFiles();
//                    System.out.println(i);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                NetworkIO.getInstance().stop();
//            }
//        });
//        read.setDaemon(true);
//        read.start();
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
        if (taFileName.getLength() > 0) {
            Network.getInstance().sendFile(Paths.get("client-storage/" + taFileName.getText()));
            taFileName.clear();
        }
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
