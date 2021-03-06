package ru.mihassu.mystorage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import ru.mihassu.mystorage.common.Constants;
import ru.mihassu.mystorage.common.FileSender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Network {

    private static Network instance = new Network();
    private static Logger logger = Logger.getLogger(Network.class.getName());
    private HandlerOperationCallback callOnAcceptData;
    private Channel channel;

    public static Network getInstance() {
        return instance;
    }

    public void setCallOnAcceptData(HandlerOperationCallback callOnAcceptData) {
        this.callOnAcceptData = callOnAcceptData;
    }

    public void start(CountDownLatch countDownLatch) {

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ChannelFuture channelFuture = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress("localhost", 8189))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel
                                    .pipeline()
                                    .addLast(new ClientFileReceiverHandler(callOnAcceptData, (userId) -> getServerFiles((int) userId)));
                            channel = socketChannel;
                        }
                    })
                    .connect()
                    .sync();
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendFile(Path path, int userId) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();

        sendTestByte(buf, Constants.UPLOAD_FILE);
        sendInt(buf, userId);
        try {
            FileSender.sendFile(path, channel, channelFuture -> {
                if (channelFuture.isSuccess()) {
                    logIt("Файл отправлен на сервер");
                } else {
                    channelFuture.cause().printStackTrace();
                }
            });
        } catch (IOException e) {
            logIt("Ошибка при отправке файла на сервер");
            e.printStackTrace();
        }
    }


    public void downloadFile(String name, int userId) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        sendTestByte(buf, Constants.DOWNLOAD_FILE);
        sendInt(buf, userId);
        sendInt(buf, name.length());
        sendFileName(buf, name);
    }

    public void deleteServerFile(String name, int userId) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        sendTestByte(buf, Constants.DELETE_FILE);
        sendInt(buf, userId);
        sendInt(buf, name.length());
        sendFileName(buf, name);
    }

    public void renameServerFile(String oldFileName, String newFileName, int userId) {
        String oldNewName = oldFileName + "/" + newFileName;
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        sendTestByte(buf, Constants.RENAME_FILE);
        sendInt(buf, userId);
        sendInt(buf, oldNewName.length());
        sendFileName(buf, oldNewName);
    }

    public void getServerFiles(int userId) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        sendTestByte(buf, Constants.REQUEST_FILES_LIST);
        sendInt(buf, userId);
    }

    public void sendAuth(String login, String password) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        String loginPass = login + "/" + password;
        sendTestByte(buf, Constants.AUTH);
        sendInt(buf, loginPass.length());
        sendFileName(buf, loginPass);
    }

    //отправить контольный байт
    public void sendTestByte(ByteBuf buf, byte testByte) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(testByte);
        channel.writeAndFlush(buf);
    }

    //отправить int
    public void sendInt(ByteBuf buf, int nameLength) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(nameLength);
        channel.writeAndFlush(buf);
    }

    //отправить имя файла
    private void sendFileName(ByteBuf buf, String name) {
        byte[] fileName = name.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(name.length());
        buf.writeBytes(fileName);
        channel.writeAndFlush(buf);
    }

    public void stop() {
        channel.close();
    }

    private static void logIt(String logText) {
        logger.log(Level.SEVERE, logText);
    }
}
