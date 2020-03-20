package ru.mihassu.mystorage;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController implements Initializable {

    private static Logger logger = Logger.getLogger(MainController.class.getName());

    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> clientFilesList;

    @FXML
    ListView<String> serverFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.getInstance().setCallOnFileSent(() -> {
            refreshFilesList(clientFilesList, "client-storage");
            refreshFilesList(serverFilesList, "server-storage");
            logIt("Callback - refresh");
        });

        final CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        try {
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        refreshFilesList(clientFilesList, "client-storage");
        refreshFilesList(serverFilesList, "server-storage");

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


    private void refreshFilesList(ListView<String> filesList, String dir) {
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

    public void onPressDisconnectBtn(ActionEvent actionEvent) {
//        NetworkIO.getInstance().stop();
        Network.getInstance().stop();
    }

    public void onPressUploadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            Network.getInstance().sendFile(Paths.get("client-storage/" + tfFileName.getText()));
            tfFileName.clear();
        }
    }

    public void onPressRefreshBtn(ActionEvent actionEvent) {
        refreshFilesList(clientFilesList, "client-storage");
        refreshFilesList(serverFilesList, "server-storage");
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
