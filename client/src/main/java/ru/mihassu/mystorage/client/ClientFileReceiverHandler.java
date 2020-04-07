package ru.mihassu.mystorage.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.mihassu.mystorage.client.ui.FileInfo;
import ru.mihassu.mystorage.common.*;

import java.util.ArrayList;
import java.util.List;

public class ClientFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private State currentState = State.IDLE;
    private int filesCount;
    private int receivedfiles;
    private String fileName;
    private int fileNameLength;
    private long fileSize;
    private String nick;
    private int userId;
    private List<FileInfo> serverFiles = new ArrayList<>();
    private HandlerOperationCallback callOnAcceptData;
    private OperationCompleteCallback fileReceived;
    private boolean loadActive = false;
    private boolean authActive = false;
    private boolean filesCountActive = false;

    public ClientFileReceiverHandler(HandlerOperationCallback callOnAcceptData, OperationCompleteCallback fileReceived) {
        this.callOnAcceptData = callOnAcceptData;
        this.fileReceived = fileReceived;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ClientFileReceiverHandler - channelRead()");

        ByteBuf buf = (ByteBuf) msg;

        while (buf.readableBytes() > 0) {

            if (currentState == State.IDLE) {
                byte testByte = buf.readByte();
                System.out.println("ClientFileReceiverHandler - testByte: " + testByte);
                if (testByte == Constants.REQUEST_FILES_LIST) {
                    currentState = State.FILES_COUNT;
                    filesCountActive = true;
                    serverFiles.clear();
                    receivedfiles = 0;
                    fileNameLength = 0;

                } else if (testByte == Constants.DOWNLOAD_FILE) {
                    loadActive = true;
                    currentState = State.LOAD_FILE;

                } else if (testByte == Constants.AUTH) {
                    authActive = true;
                    currentState = State.NAME_LENGTH;

                } else if (testByte == Constants.AUTH_FAIL) {
                    callOnAcceptData.provideData(null, null);

                } else {
                    System.out.println("ERROR: Invalid first byte - " + testByte);
                    break;
                }
            }

            if (loadActive) {
                try {
                    FileReceiver.receiveFile(buf, Constants.clientDir, () -> {
                        loadActive = false;
                        currentState = State.IDLE;
                        fileReceived.success();
                        System.out.println("ClientFileReceiverHandler - клиент получил файл");
                    });

                } catch (Exception e) {
                    System.out.println("Ошибка при получении файла клиентом: " + e.getMessage());
                    loadActive = false;
                    currentState = State.IDLE;
                }
            }

            if (authActive) {
                if (currentState == State.NAME_LENGTH || currentState == State.NAME) {
                    readFileName(buf, nick -> {
                        this.nick = (String) nick;
                        System.out.println("ClientFileReceiverHandler - получен ник от сервера: " + nick);
                        authActive = false;
                        currentState = State.IDLE;
                        callOnAcceptData.provideData(null, (String) nick);
//                        currentState = State.ID;
                    });
                }

//                if (currentState == State.ID) {
//                    if (buf.readableBytes() >= 4) {
//                        userId = buf.readInt();
//                        authActive = false;
//                        currentState = State.IDLE;
//                        callOnAcceptData.provideData(null, nick, userId);
//                    }
//                }
            }

            if (filesCountActive) {
                //Прочитать количество файлов
                if (currentState == State.FILES_COUNT) {
                    if (buf.readableBytes() >= 4) {
                        filesCount = buf.readInt();
                        currentState = State.NAME_LENGTH;
                        System.out.println("ClientFileReceiverHandler - Количество файлов нв сервере: " + filesCount);
                        if (filesCount == 0) {
                            callOnAcceptData.provideData(serverFiles, null);
                            filesCountActive = false;
                            currentState = State.IDLE;
                        }
                    }
                }

                //Прочитать имя файла
                if (currentState == State.NAME_LENGTH || currentState == State.NAME) {
                    readFileName(buf, name -> {
                        fileName = (String) name;
                        currentState = State.FILE_SIZE;
                        System.out.println("ClientFileReceiverHandler - Имя файла : " + fileName);
                    });
                }

                //прочитать размер файла
                if (currentState == State.FILE_SIZE) {
                    if (buf.readableBytes() >= 8) {
                        fileSize = buf.readLong();
                        System.out.println("ClientFileReceiverHandler - Размер файла : " + fileSize);
                        receivedfiles++;
                        serverFiles.add(new FileInfo(fileName, fileSize));
                        currentState = State.NAME_LENGTH;

                        if (receivedfiles >= filesCount) {
                            System.out.println("ClientFileReceiverHandler - Файлы на сервере: " + serverFiles.toString());
                            callOnAcceptData.provideData(serverFiles, null);
                            filesCountActive = false;
                            currentState = State.IDLE;
                        }
                    }
                }


            }
        }

        if (buf.readableBytes() == 0) {
            buf.release();
            System.out.println("ClientFileReceiverHandler - buf.release()\n====================");
        }
    }

    //readString
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
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
