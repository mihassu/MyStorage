package ru.mihassu.mystorage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClientFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private int filesCount;
    private int receivedfiles;
    private int fileNameLength;
    private List<String> files = new ArrayList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("channelRead");
//        String m = msg.toString();
//        System.out.println(m);

        ByteBuf buf = (ByteBuf) msg;

        receivedfiles = 0;
        //Прочитать количество файлов
        if (buf.readableBytes() >= 4) {
            filesCount = buf.readInt();
        }

        while (true) {
            if (filesCount == 0) break;
            System.out.println("filesCount: " + filesCount);

            if (buf.readableBytes() >= 4) {
                fileNameLength = buf.readInt();
                System.out.println("fileNameLength: " + fileNameLength);
            }
            byte[] fileName = new byte[fileNameLength];
            if (buf.readableBytes() >= fileNameLength) {
                buf.readBytes(fileName);
            }
            files.add(new String(fileName, StandardCharsets.UTF_8));
            receivedfiles++;

            if (receivedfiles >= filesCount) {
                System.out.println(files.toString());
                break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
