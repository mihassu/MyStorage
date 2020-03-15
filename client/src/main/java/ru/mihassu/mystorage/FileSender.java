package ru.mihassu.mystorage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender {

    public static void sendFile(Path path,
                                Channel channel,
                                ChannelFutureListener channelFutureListener) throws IOException {

        FileRegion region = new DefaultFileRegion(
                new FileInputStream(path.toFile()).getChannel(),
                0,
                Files.size(path));

        ByteBuf buf;

        //отправить длину имени файла
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(path.getFileName().toString().length());
        channel.writeAndFlush(buf);

        //отправить имя файла
        byte[] fileName = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(fileName.length);
        buf.writeBytes(fileName);
        channel.writeAndFlush(buf);

        //отправить размер файла
        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf);

        //отправить файл
        ChannelFuture channelFuture = channel.writeAndFlush(region);
        if (channelFutureListener != null) {
            channelFuture.addListener(channelFutureListener);
        }
    }
}
