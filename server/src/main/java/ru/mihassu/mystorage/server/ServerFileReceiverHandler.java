package ru.mihassu.mystorage.server;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.mihassu.mystorage.common.*;
import ru.mihassu.mystorage.server.db.DbAuthService;


public class ServerFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private State currentState = State.IDLE;
    private int fileNameLength;
    private String fileName;
    private int serverFilesCount;
    private int serverFileLength;
    private boolean loadActive = false;
    private boolean deleteActive = false;
    private boolean downLoadActive = false;
    private boolean authActive = false;
    private DbAuthService authService;
    private String nick;

    public ServerFileReceiverHandler(DbAuthService authService) {
        this.authService = authService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ServerFileReceiverHandler - channelRead()");
        ByteBuf buf = ((ByteBuf) msg);
        System.out.println("ServerFileReceiverHandler - readableBytes: " + buf.readableBytes());

        while (buf.readableBytes() > 0) {

            if (currentState == State.IDLE) {
                byte testByte = buf.readByte();
                System.out.println("ServerFileReceiverHandler - testByte: " + testByte);
                if (testByte == Constants.UPLOAD_FILE) {
                    loadActive = true;
                    currentState = State.LOAD_FILE;

                } else if (testByte == Constants.DOWNLOAD_FILE) {
                    downLoadActive = true;
                    currentState = State.NAME_LENGTH;

                } else if (testByte == Constants.REQUEST_FILES_LIST) {
                    currentState = State.REQUEST_FILES_LIST;

                } else if (testByte == Constants.DELETE_FILE) {
                    deleteActive = true;
                    currentState = State.NAME_LENGTH;

                } else if (testByte == Constants.AUTH) {
                    authActive = true;
                    currentState = State.NAME_LENGTH;
                }

                else {
                    System.out.println("ERROR: Invalid first byte - " + testByte);
                    break;
                }
            }

            if (loadActive) {
                try {
                    FileReceiver.receiveFile(buf, Constants.serverDir, () -> {
                        loadActive = false;
                        currentState = State.IDLE;
                        sendServerFilesList(ctx);
                        System.out.println("success() - сервер получил файл");
                    });
                } catch (Exception e) {
                    System.out.println("Ошибка при получении файла сервером: " + e.getMessage());
                    loadActive = false;
                    currentState = State.IDLE;
                }
            }

            if (downLoadActive) {
                readFileName(buf, (name) -> {
                    ctx.fireChannelRead(name);
                    downLoadActive = false;
                    currentState = State.IDLE;
                });
            }

            if (deleteActive) {
                readFileName(buf, (name) -> {
                    try {
                        Files.delete(Paths.get(Constants.serverDir + name));
                        deleteActive = false;
                        currentState = State.IDLE;
                        sendServerFilesList(ctx);
                        System.out.println("success() - файл удален");
                    } catch (IOException e) {
                        System.out.println("Ошибка при удалении файла с сервера: " + e.getMessage());
                        deleteActive = false;
                        currentState = State.IDLE;
                    }
                });
            }

            if (authActive) {
                readFileName(buf, loginPass -> {
                    String[] lp = ((String)loginPass).split("/");
                    System.out.println("ServerFileReceiverHandler - логин: " + lp[0]);
                    System.out.println("ServerFileReceiverHandler - пароль: " + lp[1]);
                    nick = authService.getNicknameByLoginPass(lp[0], lp[1]);
                    if (nick != null) {
                        confirmAuth(ctx, nick);

                    } else {
                        sendTestByte(ctx, buf, Constants.AUTH_FAIL);
                    }
                    authActive = false;
                    currentState = State.IDLE;
                });
            }

            //отправить список файлов на сервере
            if (currentState == State.REQUEST_FILES_LIST) {
                sendServerFilesList(ctx);
                currentState = State.IDLE;
            }
        }

        if (buf.readableBytes() == 0) {
            buf.release();
            System.out.println("ServerFileReceiverHandler - buf.release()\n====================");
        }
    }

    public void readFileName(ByteBuf buf, ProvideDataCallback callback) {

        //прочитать длину имени файла
        if (currentState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                fileNameLength = buf.readInt();
                currentState = State.NAME;
            }
        }

        //прочитать имя файла
        if (currentState == State.NAME) {
            if (buf.readableBytes() >= fileNameLength) {
                fileName = FileReceiver.readFileNameFromBytes(buf, fileNameLength);
                callback.provide(fileName);
                currentState = State.IDLE;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void confirmAuth(ChannelHandlerContext ctx, String nick) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        sendTestByte(ctx, buf, Constants.AUTH);
        sendInt(ctx, buf, nick.length());
        sendBytes(ctx, buf, nick.getBytes(StandardCharsets.UTF_8));
    }

    private void sendServerFilesList(ChannelHandlerContext ctx) {
        serverFilesCount = 0;
        serverFileLength = 0;
        List<byte[]> severFiles = getFilesList();
        serverFilesCount = severFiles.size();

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();

        //отправить контрольный байт
        sendTestByte(ctx, buf, Constants.REQUEST_FILES_LIST);

        //отправить количество файлов
        sendInt(ctx, buf, serverFilesCount);

        for (int i = 0; i < serverFilesCount; i++) {

            //отправить длину имени файла
            serverFileLength = severFiles.get(i).length;
            sendInt(ctx, buf, serverFileLength);

            //отправить имя файла
            sendBytes(ctx, buf, severFiles.get(i));
        }
        System.out.println("ServerFileReceiverHandler - Передано количество файлов на сервере: " + serverFilesCount);

    }

    //отправить контольный байт
    public void sendTestByte(ChannelHandlerContext ctx, ByteBuf buf, byte testByte) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(testByte);
        ctx.channel().writeAndFlush(buf);
    }

    //отправить int
    public void sendInt(ChannelHandlerContext ctx, ByteBuf buf, int nameLength) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(nameLength);
        ctx.pipeline().writeAndFlush(buf);
    }

    //отправить bytes
    private void sendBytes(ChannelHandlerContext ctx, ByteBuf buf, byte[] bytes) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buf.writeBytes(bytes);
        ctx.channel().writeAndFlush(buf);
    }

    private List<byte[]> getFilesList() {
        List<byte[]> files = new ArrayList<>();
        try {
            files = Files.list(Paths.get(Constants.serverDir))
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
