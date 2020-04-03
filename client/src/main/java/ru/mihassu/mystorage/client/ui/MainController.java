package ru.mihassu.mystorage.client.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ru.mihassu.mystorage.client.Network;
import ru.mihassu.mystorage.common.Constants;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    private static Logger logger = Logger.getLogger(MainController.class.getName());
    private boolean authentificated;
    private int userId;

    private TableView.TableViewSelectionModel<FileInfo> serverSelectionModel;
    private TableView.TableViewSelectionModel<FileInfo> clientSelectionModel;

    @FXML
    VBox authPanel;

    @FXML
    HBox workPanel;

    @FXML
    TextField fileNameField, loginField, nickField;

    @FXML
    PasswordField passField;

    @FXML
    TableView<FileInfo> serverTableView, clientTableView;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthentificated(false);
        Network.getInstance().setCallOnAcceptData((serverFiles, nick, userId) -> {
            if (serverFiles == null && nick == null) {
                System.out.println("Не удалось авторизоваться");

            } else if (serverFiles != null) {
                refreshClientList(Constants.clientDir);
                refreshServerList(serverFiles);

            } else {
                setAuthentificated(true);
                initFilesTable(serverTableView);
                initFilesTable(clientTableView);
                nickField.setText(nick);
                this.userId = userId;
                Network.getInstance().getServerFiles(userId); //обновить списки на сервере и на клиенте
                initItemsSelectedListeners();
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
    }

    private void initFilesTable(TableView<FileInfo> tableView) {
        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Имя файла");
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        fileNameColumn.setPrefWidth(200.0);

        TableColumn<FileInfo, String> fileSizeColumn = new TableColumn<>("Размер файла");
        fileSizeColumn.setCellValueFactory(param -> {
            long size = param.getValue().getSize();
            return new ReadOnlyObjectWrapper<>(String.valueOf(size) + " байт");
        });
        fileSizeColumn.setPrefWidth(100.0);

        Platform.runLater(() -> tableView.getColumns().addAll(fileNameColumn, fileSizeColumn));
    }

    private void refreshServerList(List<FileInfo> serverFiles) {
        Platform.runLater(() -> serverTableView.getItems().setAll(serverFiles));
    }

    private void refreshClientList(String dir) {

        Platform.runLater(() -> {
            try {
                List<FileInfo> fileInfoList =
                        Files.list(Paths.get(dir)) //List<Path>
                                .map(path -> {
                                    try {
                                        return new FileInfo(path.getFileName().toString(), Files.size(path));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                })
                                .collect(Collectors.toList());
                clientTableView.getItems().setAll(fileInfoList);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void onPressUploadBtn(ActionEvent actionEvent) {
        if (fileNameField.getLength() > 0) {
            String[] path = fileNameField.getText().split("/");
            if (path[0].equals("client-storage")) {
                Network.getInstance().sendFile(Paths.get(fileNameField.getText()), userId);
            } else {
                showAlert("Выберите файл на клиенте");
            }
            fileNameField.clear();
        }
    }

    public void onPressDownloadBtn(ActionEvent actionEvent) {
        if (fileNameField.getText().length() > 0) {
            String[] path = fileNameField.getText().split("/");
            if (path[0].equals("server-storage")) {
                Network.getInstance().downloadFile(path[1], userId);
            } else {
                showAlert("Выберите файл на сервере");
            }
            fileNameField.clear();
        }
    }

    public void onPressDeletedBtn(ActionEvent actionEvent) {
        String[] path = fileNameField.getText().split("/");
        switch (path[0]) {
            case "server-storage":
                Network.getInstance().deleteServerFile(path[1], userId);
                break;
            case "client-storage":
                try {
                    Files.delete(Paths.get(fileNameField.getText()));
                } catch (IOException e) {
                    System.out.println("Ошибка при удалении файла на клиенте: " + e.getMessage());
                }
                refreshClientList(Constants.clientDir);
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

    public void onPressRenameBtn(ActionEvent actionEvent) {
        try {
            new RenameWindow(newFileName -> {
                String[] path = fileNameField.getText().split("/");
                switch (path[0]) {
                    case "server-storage":
                        Network.getInstance().renameServerFile(path[1], (String) newFileName, userId);
                        break;
                    case "client-storage":
                        File file = new File(fileNameField.getText());
                        File newFile = new File(Constants.clientDir + newFileName);
                        if (file.renameTo(newFile)) {
                            System.out.println("Файл на клиенте переименован");
                        }
                        refreshClientList(Constants.clientDir);
                        break;
                }

            }).setTitle(fileNameField.getText()).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAuthentificated(boolean authentificated) {
        this.authentificated = authentificated;
        authPanel.setVisible(!authentificated); // панель с логин паролем
        authPanel.setManaged(!authentificated); // место под этот элемент
        workPanel.setVisible(authentificated);
        workPanel.setManaged(authentificated);
        nickField.setVisible(authentificated);
        nickField.setManaged(authentificated);
    }

    private void initItemsSelectedListeners() {
        clientSelectionModel = clientTableView.getSelectionModel();
        clientSelectionModel
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        fileNameField.setText(Constants.clientDir + newValue.getName());
                        serverSelectionModel.clearSelection();
                    }
                });

        serverSelectionModel = serverTableView.getSelectionModel();
        serverSelectionModel
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        fileNameField.setText(Constants.serverDir + newValue.getName());
                        clientSelectionModel.clearSelection();
                    }
                });

    }

    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(text);
        alert.setHeaderText(null);
        alert.showAndWait();
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
