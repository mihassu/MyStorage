package ru.mihassu.mystorage.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.mihassu.mystorage.common.Constants;
import ru.mihassu.mystorage.common.FileReceiver;
import ru.mihassu.mystorage.common.State;


public class ServerFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private State currentState = State.IDLE;
    private BufferedOutputStream out;
    private File fileServer;
    private int fileNameLength;
    private long fileSize;
    private long receivedFileSize;
    private int serverFilesCount;
    private int serverFileLength;
    private boolean isLoadActive = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ServerFileReceiverHandler - channelRead()");
        ByteBuf buf = ((ByteBuf) msg);
        System.out.println("ServerFileReceiverHandler - readableBytes: " + buf.readableBytes());

        while (buf.readableBytes() > 0) {

            if (isLoadActive) {
                try {
                    FileReceiver.receiveFile(buf, "server-storage/", () -> {
                        isLoadActive = false;
                        currentState = State.IDLE;
                        sendServerFilesList(ctx, buf);
                        System.out.println("loadSuccess() - сервер получил файл");
                    });
                } catch (Exception e) {
                    System.out.println("Ошибка при получении файла сервером: " + e.getMessage());
                    isLoadActive = false;
                    currentState = State.IDLE;
                }
            }

            if (currentState == State.IDLE && buf.readableBytes() > 0) {
                byte testByte = buf.readByte();
                System.out.println("ServerFileReceiverHandler - testByte: " + testByte);
                if (testByte == Constants.UPLOAD_FILE) {
                    isLoadActive = true;
                    currentState = State.LOAD_FILE;

                } else if (testByte == Constants.DOWNLOAD_FILE) {
                    currentState = State.DOWNLOAD_NAME_LENGTH;

                } else if (testByte == Constants.REQUEST_FILES_LIST) {
                    currentState = State.REQUEST_FILES_LIST;

                } else {
                    System.out.println("ERROR: Invalid first byte - " + testByte);
                    break;
                }
            }

            //прочитать длину имени файла для скачивания с сервера
            if (currentState == State.DOWNLOAD_NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    fileNameLength = buf.readInt();
                    currentState = State.DOWNLOAD_NAME;
                }
            }

            //прочитать имя файла для скачивания с сервера и передать в следующий хендлер
            if (currentState == State.DOWNLOAD_NAME) {
                if (buf.readableBytes() >= fileNameLength) {
                    byte[] fileName = new byte[fileNameLength];
                    buf.readBytes(fileName);
                    ctx.fireChannelRead(new String(fileName, StandardCharsets.UTF_8));
                    currentState = State.IDLE;
                }
            }

            //отправить список файлов на сервере
            if (currentState == State.REQUEST_FILES_LIST) {
                sendServerFilesList(ctx, buf);
                currentState = State.IDLE;
            }
        }

        if (buf.readableBytes() == 0) {
            buf.release();
            System.out.println("ServerFileReceiverHandler - buf.release()\n====================");
        }

    }

    private void sendServerFilesList(ChannelHandlerContext ctx, ByteBuf buf) {
        serverFilesCount = 0;
        serverFileLength = 0;
        List<byte[]> severFiles = getFilesList();
        serverFilesCount = severFiles.size();
        System.out.println("serverFilesCount: " + serverFilesCount);

        //отправить контрольный байт
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(Constants.REQUEST_FILES_LIST);
        ctx.channel().writeAndFlush(buf);

        //отправить количество файлов
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(serverFilesCount);
        ctx.channel().writeAndFlush(buf); //int количество файлов

        for (int i = 0; i < serverFilesCount; i++) {

            //отправить длину имени файла
            serverFileLength = severFiles.get(i).length;
            System.out.println("serverFileLength: " + serverFileLength);
            buf = ByteBufAllocator.DEFAULT.directBuffer(4);
            buf.writeInt(serverFileLength);
            ctx.channel().writeAndFlush(buf);

            //отправить имя файла
            buf = ByteBufAllocator.DEFAULT.directBuffer(serverFileLength);
            buf.writeBytes(severFiles.get(i));
            ctx.channel().writeAndFlush(buf);
            System.out.println("readableBytes(): " + buf.readableBytes());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private List<byte[]> getFilesList() {
        List<byte[]> files = new ArrayList<>();
        try {
            files = Files.list(Paths.get("server-storage/"))
                    .map(Path::toFile)
                    .map(File::getName)
                    .map(s -> s.getBytes(StandardCharsets.UTF_8))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }
}
