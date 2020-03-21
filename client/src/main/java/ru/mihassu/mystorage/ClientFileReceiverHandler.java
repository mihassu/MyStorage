package ru.mihassu.mystorage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.mihassu.mystorage.common.State;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClientFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private int filesCount;
    private int receivedfiles;
    private int fileNameLength;
    private List<String> filesNames = new ArrayList<>();
    private CallBack callOnFileSent;
    private State currentState = State.IDLE;

    public ClientFileReceiverHandler(CallBack callOnFileSent) {
        this.callOnFileSent = callOnFileSent;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ClientFileReceiverHandler - channelRead()");
        filesNames.clear();
        receivedfiles = 0;
        fileNameLength = 0;
        ByteBuf buf = (ByteBuf) msg;

        //Прочитать количество файлов
        if (buf.readableBytes() >= 4) {
            filesCount = buf.readInt();
            System.out.println("ClientFileReceiverHandler - filesCount: " + filesCount);
        }

        while (true) {
            if (filesCount == 0) break;
            //прочитать длину имени файла
            if (buf.readableBytes() >= 4) {
                fileNameLength = buf.readInt();
                System.out.println("ClientFileReceiverHandler - fileNameLength: " + fileNameLength);
            }

            //прочитать имя файла
            byte[] fileName = new byte[fileNameLength];
            if (buf.readableBytes() >= fileNameLength) {
                buf.readBytes(fileName);
            }

            //добавить имя файла в массив
            filesNames.add(new String(fileName, StandardCharsets.UTF_8));
            receivedfiles++;

            if (receivedfiles >= filesCount) {
                System.out.println("ClientFileReceiverHandler - fileNames" + filesNames.toString());
                callOnFileSent.refreshList(filesNames);
                break;
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
            System.out.println("ClientFileReceiverHandler - buf.release()");
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
