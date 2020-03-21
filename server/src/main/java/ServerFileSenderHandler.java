import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerFileSenderHandler extends ChannelOutboundHandlerAdapter {

    private int serverFilesCount;
    private int serverFileLength;
    private List<byte[]> severFiles = new ArrayList<>();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("ServerFileSenderHandler - write()");
        ByteBuf outBuf;
        serverFilesCount = 0;
        serverFileLength = 0;
        severFiles = getFilesList();
        serverFilesCount = severFiles.size();
        System.out.println("serverFilesCount: " + serverFilesCount);

        //отправить количество файлов
        outBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        outBuf.writeInt(serverFilesCount);
        ctx.channel().writeAndFlush(outBuf); //int количество файлов

        for (int i = 0; i < serverFilesCount; i++) {

            //отправить длину имени файла
            serverFileLength = severFiles.get(i).length;
            System.out.println("serverFileLength: " + serverFileLength);
            outBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
            outBuf.writeInt(serverFileLength);
            ctx.channel().writeAndFlush(outBuf);

            //отправить имя файла
            outBuf = ByteBufAllocator.DEFAULT.directBuffer(serverFileLength);
            outBuf.writeBytes(severFiles.get(i));
            ctx.channel().writeAndFlush(outBuf);
            System.out.println("readableBytes(): " + outBuf.readableBytes());
        }
    }

//    @Override
//    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//        ctx.disconnect();
//    }
//
//    @Override
//    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//        ctx.close();
//    }

    private List<byte[]> getFilesList() {
        List<byte[]> files = new ArrayList<>();
        try {
            files = Files.list(Paths.get("server-storage/"))
                    .map(Path::toFile)
                    .map(File::getName)
                    .map(s -> s.getBytes(StandardCharsets.UTF_8))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }
}
