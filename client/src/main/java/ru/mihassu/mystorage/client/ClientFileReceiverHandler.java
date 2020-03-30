package ru.mihassu.mystorage.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.mihassu.mystorage.common.Constants;
import ru.mihassu.mystorage.common.FileReceiver;
import ru.mihassu.mystorage.common.OperationCompleteCallback;
import ru.mihassu.mystorage.common.State;

import java.util.ArrayList;
import java.util.List;

public class ClientFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private State currentState = State.IDLE;
    private int filesCount;
    private int receivedfiles;
    private String fileName;
    private int fileNameLength;
    private List<String> filesNames = new ArrayList<>();
    private RefreshListCallback callOnListRefresh;
    private OperationCompleteCallback fileReceived;
    private boolean loadActive = false;

    public ClientFileReceiverHandler(RefreshListCallback callOnListRefresh, OperationCompleteCallback fileReceived) {
        this.callOnListRefresh = callOnListRefresh;
        this.fileReceived = fileReceived;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ClientFileReceiverHandler - channelRead()");

        ByteBuf buf = (ByteBuf) msg;

        while (buf.readableBytes() > 0) {

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
                } else {
                    System.out.println("ERROR: Invalid first byte - " + testByte);
                    break;
                }
            }

            //Прочитать количество файлов
            if (currentState == State.FILES_COUNT) {
                if (buf.readableBytes() >= 4) {
                    filesCount = buf.readInt();
                    currentState = State.NAME_LENGTH;
                    System.out.println("ClientFileReceiverHandler - filesCount: " + filesCount);
                    if (filesCount == 0) {
                        callOnListRefresh.refreshList(filesNames);
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
                            fileName = FileReceiver.readFileName(buf, fileNameLength);
                            //добавить имя файла в массив
                            filesNames.add(fileName);
                            receivedfiles++;
                            currentState = State.NAME_LENGTH;
                        }

                        if (receivedfiles >= filesCount) {
                            System.out.println("ClientFileReceiverHandler - fileNames" + filesNames.toString());
                            callOnListRefresh.refreshList(filesNames);
                            currentState = State.IDLE;
                            break;
                        }
                    }
                }
            }

        }
        if (buf.readableBytes() == 0) {
            buf.release();
            System.out.println("ClientFileReceiverHandler - buf.release()");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
