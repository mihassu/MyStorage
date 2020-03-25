package ru.mihassu.mystorage.common;

import io.netty.buffer.ByteBuf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileReceiver {

    private static State currentState = State.IDLE;
    private static BufferedOutputStream out;
    private static File file;
    private static int fileNameLength;
    private static long fileSize;
    private static long receivedFileSize;

    public static void receiveFile(ByteBuf buf, String dir, LoadSuccessCallback callback) throws Exception {

        if (currentState == State.IDLE) {
            receivedFileSize = 0L;
            currentState = State.NAME_LENGTH;
        }

        //прочитать длину имени файла
        if (currentState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                fileNameLength = buf.readInt();
                currentState = State.NAME;
                System.out.println("FileReceiver - fileNameLength: " + fileNameLength);
            }
        }

        //прочитать имя файла и создать поток для записи файла
        if (currentState == State.NAME) {
            if (buf.readableBytes() >= fileNameLength) {
                byte[] fileName = new byte[fileNameLength];
                buf.readBytes(fileName);
                file = new File(dir + new String(fileName, StandardCharsets.UTF_8));
                out = new BufferedOutputStream(new FileOutputStream(file));
                currentState = State.FILE_SIZE;
                System.out.println("FileReceiver - file: " + file.getName());
            }
        }

        //прочитать размер файла
        if (currentState == State.FILE_SIZE) {
            if (buf.readableBytes() >= 8) {
                fileSize = buf.readLong();
                currentState = State.FILE;
                System.out.println("FileReceiver - fileSize: " + fileSize);
            }
        }

        //прочитать файл
        if (currentState == State.FILE) {
            try {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileSize++;
                    if (receivedFileSize == fileSize) {
                        currentState = State.IDLE;
                        out.close();
                        callback.loadSuccess();
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getCause().getMessage());
            }
        }
    }
}
