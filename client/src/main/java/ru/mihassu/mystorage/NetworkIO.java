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
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkIO {

    private static NetworkIO instance = new NetworkIO();
    private static Logger logger = Logger.getLogger(NetworkIO.class.getName());
    private CallBack callOnFileSent;

    public void setCallOnFileSent(CallBack callOnFileSent) {
        this.callOnFileSent = callOnFileSent;
    }

    public static NetworkIO getInstance() {
        return instance;
    }


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
    }

    public void sendFile(Path path) {
        try {
            FileSenderIO.sendFile(path, out);
        } catch (IOException e) {
            logIt("Ошибка при отправке файла");
            e.printStackTrace();
        }
    }

    public int readServerFiles() throws IOException {
        System.out.println("readServerFiles()");
        int i = in.readInt();
        System.out.println(i);
        return i;

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
    }

    private static void logIt(String logText) {
        logger.log(Level.SEVERE, logText);
    }
}
