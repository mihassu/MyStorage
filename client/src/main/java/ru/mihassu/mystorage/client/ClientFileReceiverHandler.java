package ru.mihassu.mystorage.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.mihassu.mystorage.common.*;

import java.util.ArrayList;
import java.util.List;

public class ClientFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private State currentState = State.IDLE;
    private int filesCount;
    private int receivedfiles;
    private String fileName;
    private int fileNameLength;
    private List<String> filesNames = new ArrayList<>();
    private HandlerOperationCallback callOnAcceptData;
    private OperationCompleteCallback fileReceived;
    private boolean loadActive = false;
    private boolean authActive = false;

    public ClientFileReceiverHandler(HandlerOperationCallback callOnAcceptData, OperationCompleteCallback fileReceived) {
        this.callOnAcceptData = callOnAcceptData;
        this.fileReceived = fileReceived;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ClientFileReceiverHandler - channelRead()");

        ByteBuf buf = (ByteBuf) msg;

        while (buf.readableBytes() > 0) {

            if (currentState == State.IDLE && buf.readableBytes() > 0) {
                byte testByte = buf.readByte();
                System.out.println("ClientFileReceiverHandler - testByte: " + testByte);
                if (testByte == Constants.REQUEST_FILES_LIST) {
                    currentState = State.FILES_COUNT;
                    filesNames.clear();
                    receivedfiles = 0;
                    fileNameLength = 0;
                } else if (testByte == Constants.DOWNLOAD_FILE) {
                    loadActive = true;
                    currentState = State.LOAD_FILE;
                } else if (testByte == Constants.AUTH) {
                    authActive = true;
                    currentState = State.NAME_LENGTH;

                }  else {
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
                        System.out.println("loadSuccess() - клиент получил файл");
                    });

                } catch (Exception e) {
                    System.out.println("Ошибка при получении файла клиентом: " + e.getMessage());
                    loadActive = false;
                    currentState = State.IDLE;
                }
            }

            if (authActive) {
                readFileName(buf, nick -> {
                    System.out.println("ClientFileReceiverHandler - получен ник от сервера: " + nick);
                    authActive = false;
                    currentState = State.IDLE;
                    callOnAcceptData.provideData(null, (String) nick);
                });
            }

            //Прочитать количество файлов
            if (currentState == State.FILES_COUNT) {
                if (buf.readableBytes() >= 4) {
                    filesCount = buf.readInt();
                    currentState = State.NAME_LENGTH;
                    System.out.println("ClientFileReceiverHandler - filesCount: " + filesCount);
                    if (filesCount == 0) {
                        callOnAcceptData.provideData(filesNames, null);
                        currentState = State.IDLE;
                    }
                }
            }

            if (currentState == State.NAME_LENGTH || currentState == State.NAME) {
                while (buf.readableBytes() > 0) {
                    //прочитать длину имени файла
                    if (currentState == State.NAME_LENGTH) {
                        if (buf.readableBytes() >= 4) {
                            fileNameLength = buf.readInt();
                            currentState = State.NAME;
                            System.out.println("ClientFileReceiverHandler - fileNameLength: " + fileNameLength);
                        }
                    }

                    if (currentState == State.NAME) {
                        //прочитать имя файла
                        if (buf.readableBytes() >= fileNameLength) {
                            fileName = FileReceiver.readFileNameFromBytes(buf, fileNameLength);
                            //добавить имя файла в массив
                            filesNames.add(fileName);
                            receivedfiles++;
                            currentState = State.NAME_LENGTH;
                        }

                        if (receivedfiles >= filesCount) {
                            System.out.println("ClientFileReceiverHandler - fileNames" + filesNames.toString());
                            callOnAcceptData.provideData(filesNames, null);
                            currentState = State.IDLE;
                            break;
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
                currentState = State.IDLE;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
