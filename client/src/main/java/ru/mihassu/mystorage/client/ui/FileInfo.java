package ru.mihassu.mystorage.client.ui;


import java.io.File;

public class FileInfo {

    private File file;
    private String name;
    private long size;

    public FileInfo(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
