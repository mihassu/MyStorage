package ru.mihassu.mystorage.server;

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
    private long serverFileSize;
    private boolean loadActive = false;
    private boolean deleteActive = false;
    private boolean downLoadActive = false;
    private boolean authActive = false;
    private boolean renameActive = false;
    private DbAuthService authService;
    private String currentNick;
    private int currentUserId;
    private String currentDir;

    public ServerFileReceiverHandler(DbAuthService authService) {
        this.authService = authService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
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
                    currentState = State.ID;

                } else if (testByte == Constants.DOWNLOAD_FILE) {
                    downLoadActive = true;
                    currentState = State.ID;

                } else if (testByte == Constants.REQUEST_FILES_LIST) {
                    currentState = State.REQUEST_FILES_LIST;

                } else if (testByte == Constants.DELETE_FILE) {
                    deleteActive = true;
                    currentState = State.ID;

                } else if (testByte == Constants.RENAME_FILE) {
                    renameActive = true;
                    currentState = State.ID;

                } else if (testByte == Constants.AUTH) {
                    authActive = true;
                    currentState = State.NAME_LENGTH;
                } else {
                    System.out.println("ERROR: Invalid first byte - " + testByte);
                    break;
                }
            }

            if (loadActive) {
                if (currentState == State.ID) {
                    if (buf.readableBytes() >= 4) {
                        currentUserId = buf.readInt();
                        currentNick = authService.getNicknameById(currentUserId);
                        currentDir = Constants.serverDir + "/" + currentNick + "/"; // server-storage/userA/
                        currentState = State.LOAD_FILE;
                    }
                }

                if (currentState == State.LOAD_FILE) {
                    try {
                        FileReceiver.receiveFile(buf, currentDir, () -> {
                            loadActive = false;
                            currentState = State.IDLE;
                            sendServerFilesList(ctx, currentNick);
                            System.out.println("success() - сервер получил файл");
                        });
                    } catch (Exception e) {
                        System.out.println("Ошибка при получении файла сервером: " + e.getMessage());
                        loadActive = false;
                        currentState = State.IDLE;
                    }
                }
            }

            if (downLoadActive) {
                if (currentState == State.ID) {
                    if (buf.readableBytes() >= 4) {
                        currentUserId = buf.readInt();
                        currentNick = authService.getNicknameById(currentUserId);
                        currentDir = Constants.serverDir + "/" + currentNick; //server-storage/userA
                        currentState = State.NAME_LENGTH;
                    }
                }

                if (currentState == State.NAME_LENGTH || currentState == State.NAME) {
                    readFileName(buf, (name) -> {
                        String fileDir = currentDir + "/" + name; //server-storage/userA/name.txt
                        ctx.fireChannelRead(fileDir);
                        downLoadActive = false;
                        currentState = State.IDLE;
                    });
                }
            }

            if (deleteActive) {
                if (currentState == State.ID) {
                    if (buf.readableBytes() >= 4) {
                        currentUserId = buf.readInt();
                        currentNick = authService.getNicknameById(currentUserId);
                        currentDir = Constants.serverDir + "/" + currentNick; //server-storage/userA
                        currentState = State.NAME_LENGTH;
                    }
                }

                if (currentState == State.NAME_LENGTH || currentState == State.NAME) {
                    readFileName(buf, (name) -> {
                        try {
                            Files.delete(Paths.get(currentDir + "/" + name));
                            deleteActive = false;
                            currentState = State.IDLE;
                            sendServerFilesList(ctx, currentNick);
                            System.out.println("success() - файл на сервере удален");
                        } catch (IOException e) {
                            System.out.println("Ошибка при удалении файла с сервера: " + e.getMessage());
                            deleteActive = false;
                            currentState = State.IDLE;
                        }
                    });
                }
            }

            if (renameActive) {
                if (currentState == State.ID) {
                    if (buf.readableBytes() >= 4) {
                        currentUserId = buf.readInt();
                        currentNick = authService.getNicknameById(currentUserId);
                        currentDir = Constants.serverDir + "/" + currentNick; //server-storage/userA
                        currentState = State.NAME_LENGTH;
                    }
                }

                if (currentState == State.NAME_LENGTH || currentState == State.NAME) {
                    readFileName(buf, (name) -> {
                        String[] oldNew = ((String) name).split("/");
                        File oldFile = new File(currentDir + "/" + oldNew[0]);
                        File newFile = new File(currentDir + "/" + oldNew[1]);
                        if (oldFile.renameTo(newFile)) {
                            sendServerFilesList(ctx, currentNick);
                            System.out.println("Файл на сервере переименован");

                        } else {
                            System.out.println("Ошибка при переименовании файла на сервере");
                        }

                        renameActive = false;
                        currentState = State.IDLE;
                    });
                }
            }

            if (authActive) {
                readFileName(buf, loginPass -> {
                    String[] lp = ((String) loginPass).split("/");
                    System.out.println("ServerFileReceiverHandler - логин: " + lp[0]);
                    System.out.println("ServerFileReceiverHandler - пароль: " + lp[1]);
                    currentNick = authService.getNicknameByLoginPass(lp[0], lp[1]);
                    currentUserId = authService.getIdByLoginPass(lp[0], lp[1]);
                    if (currentNick != null) {
                        confirmAuth(ctx, currentNick, currentUserId);
                        createUserDirectory(currentNick);

                    } else {
                        sendTestByte(ctx, buf, Constants.AUTH_FAIL);
                    }
                    authActive = false;
                    currentState = State.IDLE;
                });
            }

            //отправить список файлов на сервере
            if (currentState == State.REQUEST_FILES_LIST) {
                if (buf.readableBytes() >= 4) {
                    currentUserId = buf.readInt();
                    currentNick = authService.getNicknameById(currentUserId);
                    sendServerFilesList(ctx, currentNick);
                    currentState = State.IDLE;
                }
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

    private void confirmAuth(ChannelHandlerContext ctx, String nick, int userId) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        sendTestByte(ctx, buf, Constants.AUTH);
        sendInt(ctx, buf, nick.length());
        sendBytes(ctx, buf, nick);
        sendInt(ctx, buf, userId);
    }

    private void createUserDirectory(String userName) {
        Path path = Paths.get(Constants.serverDir + "/" + userName);
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            System.out.println("ServerFileReceiverHandler - Ошибка при создании папки: " + e.getMessage());
        }
    }

    private void sendServerFilesList(ChannelHandlerContext ctx, String nick) {
        serverFilesCount = 0;
        serverFileLength = 0;
        List<Path> severFiles = getFilesList(nick);
        serverFilesCount = severFiles.size();

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();

        //отправить контрольный байт
        sendTestByte(ctx, buf, Constants.REQUEST_FILES_LIST);

        //отправить количество файлов
        sendInt(ctx, buf, serverFilesCount);

        for (int i = 0; i < serverFilesCount; i++) {

            //отправить длину имени файла
            serverFileLength = severFiles.get(i).toFile().getName().length();
            System.out.println("ServerFileReceiverHandler - Длина имени файла: " + serverFileLength);
            sendInt(ctx, buf, serverFileLength);

            //отправить имя файла
            fileName = severFiles.get(i).toFile().getName();
            System.out.println("ServerFileReceiverHandler - Имя файла: " + fileName);
            sendBytes(ctx, buf, fileName);

            //отправить размер файла
            try {
                serverFileSize = Files.size(severFiles.get(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("ServerFileReceiverHandler - Размер файла: " + serverFileSize);
            buf = ByteBufAllocator.DEFAULT.directBuffer(8);
            buf.writeLong(serverFileSize);
            ctx.channel().writeAndFlush(buf);
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
        ctx.channel().writeAndFlush(buf);
    }

    //отправить bytes
    private void sendBytes(ChannelHandlerContext ctx, ByteBuf buf, String name) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(nameBytes.length);
        buf.writeBytes(nameBytes);
        ctx.channel().writeAndFlush(buf);
    }

    private List<Path> getFilesList(String nick) {
        List<Path> files = new ArrayList<>();
        try {
            files = Files.list(Paths.get(Constants.serverDir + "/" + nick))
//                    .map(Path::toFile)
//                    .map(File::getName)
//                    .map(s -> s.getBytes(StandardCharsets.UTF_8))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }
}
