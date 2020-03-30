package ru.mihassu.mystorage.common;

public enum State {
    IDLE,
    NAME_LENGTH,
    NAME,
    FILE_SIZE,
    FILE,
    FILES_COUNT,
    REQUEST_FILES_LIST,
    LOAD_FILE,
    AUTH
}
