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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - channelRead()");
        ByteBuf buf = ((ByteBuf) msg);
        System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - readableBytes: " + buf.readableBytes());

        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte testByte = buf.readByte();
                System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - testByte: " + testByte);
                if (testByte == Constants.UPLOAD_FILE) {
                    currentState = State.NAME_LENGTH;
                    receivedFileSize = 0L;

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

            //прочитать длину имени файла для загрузки на сервер
            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    fileNameLength = buf.readInt();
                    currentState = State.NAME;
                    System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - fileNameLength: " + fileNameLength);
                }
            }

            //прочитать имя файла и создать пустой файл на сервере
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= fileNameLength) {
                    byte[] fileName = new byte[fileNameLength];
                    buf.readBytes(fileName);
                    fileServer = new File("server-storage/" + new String(fileName, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream(fileServer));
                    currentState = State.FILE_SIZE;
                    System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - fileServer: " + fileServer.getName());
                }
            }

            //прочитать размер файла
            if (currentState == State.FILE_SIZE) {
                System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - State.FILE_LENGTH. readableBytes: " + buf.readableBytes());
                if (buf.readableBytes() >= 8) {
                    fileSize = buf.readLong();
                    currentState = State.FILE;
                    System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - fileSize: " + fileSize);
                }
            }

            //прочитать файл
            if (currentState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileSize++;
                    if (receivedFileSize == fileSize) {
                        currentState = State.IDLE;
                        out.close();
                        sendServerFilesList(ctx, buf);
                        break;
                    }
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
            System.out.println("ru.mihassu.mystorage.server.ServerFileReceiverHandler - buf.release()\n====================");
        }

    }

    private void sendServerFilesList(ChannelHandlerContext ctx, ByteBuf outBuf) {
        serverFilesCount = 0;
        serverFileLength = 0;
        List<byte[]> severFiles = getFilesList();
        serverFilesCount = severFiles.size();
        System.out.println("serverFilesCount: " + serverFilesCount);

        //отправить контрольный байт
        outBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
        outBuf.writeByte(Constants.REQUEST_FILES_LIST);
        ctx.channel().writeAndFlush(outBuf);

        //отправить количество файлов
        outBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        outBuf.writeInt(serverFilesCount);
        ctx.channel().writeAndFlush(outBuf); //int количество файлов

        for (int i = 0; i < serverFilesCount; i++) {

            //отправить длину имени файла
            serverFileLength = severFiles.get(i).length;
            System.out.println("serverFileLength: " + serverFileLength);
            outBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
            outBuf.writeInt(serverFileLength);
            ctx.channel().writeAndFlush(outBuf);

            //отправить имя файла
            outBuf = ByteBufAllocator.DEFAULT.directBuffer(serverFileLength);
            outBuf.writeBytes(severFiles.get(i));
            ctx.channel().writeAndFlush(outBuf);
            System.out.println("readableBytes(): " + outBuf.readableBytes());
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
