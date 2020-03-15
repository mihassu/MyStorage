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

    public static void sendFile(Path path,
                                DataOutputStream out,
                                ChannelFutureListener channelFutureListener) throws IOException {

        long fileSize = Files.size(path);

        //отправить длину имени файла
        out.writeInt(path.getFileName().toString().length());
        out.flush();

        //отправить имя файла
        byte[] fileName = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        out.write(fileName);
        out.flush();

        //отправить размер файла
        out.writeLong(fileSize);
        out.flush();

        //отправить файл
        File file = new File(path.toString());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            long byteCount = 0L;
            while (true) {
                out.writeByte(fileInputStream.read());
                byteCount++;
                if (byteCount == fileSize) {
                    logIt("Отправлен файл: " + path.toString());
                    break;
                }
            }
        }

//        FileRegion region = new DefaultFileRegion(
////                new FileInputStream(path.toFile()).getChannel(),
////                0,
////                Files.size(path));

//        ByteBuf buf;
//        //отправить длину имени файла
//        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
//        buf.writeInt(path.getFileName().toString().length());
//        channel.writeAndFlush(buf);
//
//        //отправить имя файла
//        byte[] fileName = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
//        buf = ByteBufAllocator.DEFAULT.directBuffer(fileName.length);
//        buf.writeBytes(fileName);
//        channel.writeAndFlush(buf);
//
//        //отправить размер файла
//        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
//        buf.writeLong(Files.size(path));
//        channel.writeAndFlush(buf);
//
//        //отправить файл
//        ChannelFuture channelFuture = channel.writeAndFlush(region);
//        if (channelFutureListener != null) {
//            channelFuture.addListener(channelFutureListener);
//        }
    }

    private static void logIt(String logText) {
        logger.log(Level.SEVERE, logText);
    }
}
