package ru.mihassu.mystorage.client;

import java.util.List;

public interface RefreshListCallback {
    void refreshList(List<String> filesNames);
}
