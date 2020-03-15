package ru.mihassu.mystorage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Network {

    private static Network instance = new Network();
    private static Logger logger = Logger.getLogger(Network.class.getName());
    private CallBack callOnFileSent;

    public void setCallOnFileSent(CallBack callOnFileSent) {
        this.callOnFileSent = callOnFileSent;
    }

    public static Network getInstance() {
        return instance;
    }

//    private Channel channel;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public void start(CountDownLatch countDownLatch) {

        try {
            socket = new Socket("localhost", 8189);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            countDownLatch.countDown();

        } catch (IOException e) {
            e.printStackTrace();
            stop();
        }

//        EventLoopGroup group = new NioEventLoopGroup();
//        try {
//            ChannelFuture channelFuture = new Bootstrap()
//                    .group(group)
//                    .channel(NioSocketChannel.class)
//                    .remoteAddress(new InetSocketAddress("localhost", 8189))
//                    .handler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        protected void initChannel(SocketChannel socketChannel) throws Exception {
//                            socketChannel.pipeline().addLast();
//                            channel = socketChannel;
//                        }
//                    })
//                    .connect()
//                    .sync();
//            countDownLatch.countDown();
//            channelFuture.channel().closeFuture().sync();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                group.shutdownGracefully().sync();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void sendFile(Path path) {
        try {
            FileSender.sendFile(path, out, channelFuture -> {
                if (channelFuture.isSuccess()) {
                    logIt("Файл отправлен");
                    callOnFileSent.refreshList();
                } else {
                    channelFuture.cause().printStackTrace();
                }
            });
        } catch (IOException e) {
            logIt("Ошибка при отправке файла");
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        channel.close();
    }

    private static void logIt(String logText) {
        logger.log(Level.SEVERE, logText);
    }
}
