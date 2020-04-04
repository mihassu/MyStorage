package ru.mihassu.mystorage.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.mihassu.mystorage.common.Constants;
import ru.mihassu.mystorage.common.FileSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerFileSenderHandler extends ChannelInboundHandlerAdapter {

    private Path file;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ServerFileSenderHandler - channelRead()");
        String name = (String) msg;
        System.out.println("ServerFileSenderHandler - Файл для отправки: " + name);
        file = Paths.get(name);
        if (Files.exists(file)) {
            ByteBuf buf;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeByte(Constants.DOWNLOAD_FILE);
            ctx.channel().writeAndFlush(buf);

            FileSender.sendFile(file, ctx.channel(), channelFuture -> {
                if (channelFuture.isSuccess()) {
                    System.out.println("Файл " + name + " отправлен клиенту");
                } else {
                    channelFuture.cause().printStackTrace();
                }
            });
        }

//        if (fileExist(name)) {
//            file = Paths.get(Constants.serverDir + name);
//            FileSender.sendFile(file, ctx.channel(), Constants.DOWNLOAD_FILE, channelFuture -> {
//                if (channelFuture.isSuccess()) {
//                    System.out.println("Файл " + name + " отправлен клиенту");
//                } else {
//                    channelFuture.cause().printStackTrace();
//                }
//            });
//        }
    }

    private boolean fileExist(String fileName) {
        return Files.exists(Paths.get(fileName));
//        try {
//            return Files.list(Paths.get(Constants.serverDir))
//                    .map(Path::toFile)
//                    .map(File::getName)
//                    .anyMatch(name -> name.equals(fileName));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
    }
}
