package ru.mihassu.mystorage.common;

public interface Constants {
    byte UPLOAD_FILE = 13; //загрузить на сервер
    byte DOWNLOAD_FILE = 14; //скачать с сервера
    byte REQUEST_FILES_LIST = 9; //запросить список фалов
    byte DELETE_FILE = 8; //удалить файл
    byte AUTH = 3;
    byte AUTH_FAIL = 2;
    byte RENAME_FILE = 5;

    String clientDir = "client-storage";
    String serverDir = "server-storage";

}
