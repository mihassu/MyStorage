package ru.mihassu.mystorage.client;

import ru.mihassu.mystorage.client.ui.FileInfo;

import java.util.List;

public interface HandlerOperationCallback {
    void provideData(List<FileInfo> filesNames, String nick);
}
