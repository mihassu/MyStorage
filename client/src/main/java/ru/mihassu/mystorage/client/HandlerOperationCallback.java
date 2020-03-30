package ru.mihassu.mystorage.client;

import java.util.List;

public interface HandlerOperationCallback {
    void provideData(List<String> filesNames, String nick);
}
