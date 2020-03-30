package ru.mihassu.mystorage.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private boolean authentificated;

    @FXML
    VBox authPanel;

    @FXML
    HBox workPanel;

    @FXML
    TextField fileNameField, loginField;

    @FXML
    PasswordField passField;

    @FXML
    ListView<String> clientFilesList, serverFilesList;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthentificated(false);
        Network.getInstance().setCallOnAcceptData((filesNames, nick) -> {
            if (filesNames == null && nick == null) {
                System.out.println("Не удалось авторизоваться");

            } else if (filesNames != null) {
                refreshClientList(clientFilesList, Constants.clientDir);
                refreshServerList(filesNames);
//            logIt("Callback - refresh");

            } else {
                setAuthentificated(true);
                System.out.println("Авторизация выполнена. Ник: " + nick);
            }
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
            if (filesNames.size() > 0) {
                for (String f : filesNames) {
                    serverFilesList.getItems().add(f);
                }
            } else {
                serverFilesList.getItems().add("Пусто");
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
            String[] path = fileNameField.getText().split("/");
            if (path[0].equals("client-storage")) {
                Network.getInstance().sendFile(Paths.get(fileNameField.getText()));
            } else {
                System.out.println("Выберите файл на клиенте");
            }
            fileNameField.clear();
        }
    }

    public void onPressDownloadBtn(ActionEvent actionEvent) {
        if (fileNameField.getText().length() > 0) {
            String[] path = fileNameField.getText().split("/");
            if (path[0].equals("server-storage")) {
                Network.getInstance().downloadFile(path[1]);
            } else {
                System.out.println("Выберите файл на сервере");
            }
            fileNameField.clear();
        }
    }

    public void onPressDeletedBtn(ActionEvent actionEvent) {
        String[] path = fileNameField.getText().split("/");
        switch (path[0]) {
            case "server-storage":
                Network.getInstance().deleteServerFile(path[1]);
                break;
            case "client-storage":
                try {
                    Files.delete(Paths.get(fileNameField.getText()));
                } catch (IOException e) {
                    System.out.println("Ошибка при удалении файла на клиенте: " + e.getMessage());
                }
                refreshClientList(clientFilesList, Constants.clientDir);
                break;
        }
    }

    public void onPressAuthBtn(ActionEvent actionEvent) {
        Network.getInstance().sendAuth(loginField.getText(), passField.getText());
        loginField.clear();
        passField.clear();
    }

    public void onPressDisconnectBtn(ActionEvent actionEvent) {
        setAuthentificated(false);
    }

    public void setAuthentificated(boolean authentificated) {
        this.authentificated = authentificated;
        authPanel.setVisible(!authentificated); // панель с логин паролем
        authPanel.setManaged(!authentificated); // место под этот элемент
        workPanel.setVisible(authentificated);
        workPanel.setManaged(authentificated);
        fileNameField.setVisible(authentificated);
        fileNameField.setManaged(authentificated);
    }

    private void initItemsSelectedListeners() {
        MultipleSelectionModel<String> clientSelectionModel = clientFilesList.getSelectionModel();
        clientSelectionModel
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> fileNameField.setText(Constants.clientDir + newValue));

        MultipleSelectionModel<String> serverSelectionModel = serverFilesList.getSelectionModel();
        serverSelectionModel
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> fileNameField.setText(Constants.serverDir + newValue));
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
