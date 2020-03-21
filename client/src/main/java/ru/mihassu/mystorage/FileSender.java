package ru.mihassu.mystorage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSender {

    private static Logger logger = Logger.getLogger(FileSender.class.getName());
    public static final byte TEST_BYTE = 13;

    public static void sendFile(Path path,
                                Channel channel,
                                ChannelFutureListener channelFutureListener) throws IOException {

        FileRegion region = new DefaultFileRegion(
                new FileInputStream(path.toFile()).getChannel(),
                0,
                Files.size(path));

        ByteBuf buf;

        //отправить контольный байт
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(TEST_BYTE);
        channel.writeAndFlush(buf);

        //отправить длину имени файла
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(path.getFileName().toString().length());
        channel.writeAndFlush(buf);
        System.out.println("FileSender - fileLength: " + path.getFileName().toString().length());

        //отправить имя файла
        byte[] fileName = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(fileName.length);
        buf.writeBytes(fileName);
        channel.writeAndFlush(buf);
        System.out.println("FileSender - fileName: " + new String(fileName, StandardCharsets.UTF_8));

        //отправить размер файла
        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf);
        System.out.println("FileSender - fileSize: " + Files.size(path));

        //отправить файл
        ChannelFuture channelFuture = channel.writeAndFlush(region);
        if (channelFutureListener != null) {
            channelFuture.addListener(channelFutureListener);
        }

    }

    private static void logIt(String logText) {
        logger.log(Level.SEVERE, logText);
    }
}
