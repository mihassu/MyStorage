package ru.mihassu.mystorage.server;

import io.netty.buffer.ByteBuf;
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

        if (fileExist(name)) {
            file = Paths.get(Constants.serverDir + name);
            FileSender.sendFile(file, ctx.channel(), Constants.DOWNLOAD_FILE, channelFuture -> {
                if (channelFuture.isSuccess()) {
                    System.out.println("Файл " + name + " отправлен клиенту");
                } else {
                    channelFuture.cause().printStackTrace();
                }
            });
        }
    }

    private boolean fileExist(String fileName) {
        try {
            return Files.list(Paths.get(Constants.serverDir))
                    .map(Path::toFile)
                    .map(File::getName)
                    .anyMatch(name -> name.equals(fileName));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
