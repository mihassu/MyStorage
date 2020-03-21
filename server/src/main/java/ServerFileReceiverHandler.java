import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

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
import ru.mihassu.mystorage.common.State;


public class ServerFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private State currentState = State.IDLE;
    public static final byte TEST_BYTE = 13;
    private BufferedOutputStream out;
    private File fileServer;
    private int fileNameLength;
    private long fileSize;
    private long receivedFileSize;
    private boolean isFileReceived = false;

    private int serverFilesCount;
    private int serverFileLength;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ServerFileReceiverHandler - channelRead()");
        ByteBuf buf = ((ByteBuf) msg);
        System.out.println("ServerFileReceiverHandler - readableBytes: " + buf.readableBytes());

//        if (isFileReceived) {
//            ctx.pipeline().fireChannelActive();
//            isFileReceived = false;
//            return;
//        }

        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte testByte = buf.readByte();
                System.out.println("ServerFileReceiverHandler - testByte: " + testByte);
                if (testByte == TEST_BYTE) {
                    currentState = State.NAME_LENGTH;
                    receivedFileSize = 0L;
                } else {
                    System.out.println("ERROR: Invalid first byte - " + testByte);
                    break;
                }
            }

            //прочитать длину имени файла
            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    fileNameLength = buf.readInt();
                    currentState = State.NAME;
                    System.out.println("ServerFileReceiverHandler - fileNameLength: " + fileNameLength);
                }
            }

            //прочитать имя файла и создать файл
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= fileNameLength) {
                    byte[] fileName = new byte[fileNameLength];
                    buf.readBytes(fileName);
                    fileServer = new File("server-storage/" + new String(fileName, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream(fileServer));
                    currentState = State.FILE_LENGTH;
                    System.out.println("ServerFileReceiverHandler - fileServer: " + fileServer.getName());
                }
            }

            //прочитать размер файла
            if (currentState == State.FILE_LENGTH) {
                System.out.println("ServerFileReceiverHandler - State.FILE_LENGTH. readableBytes: " + buf.readableBytes());
                if (buf.readableBytes() >= 8) {
                    fileSize = buf.readLong();
                    currentState = State.FILE;
                    System.out.println("ServerFileReceiverHandler - fileSize: " + fileSize);
                }
            }

            //прочитать файл
            if (currentState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileSize++;
                    if (receivedFileSize == fileSize) {
                        currentState = State.IDLE;
                        out.close();
                        sendServerFilesList(ctx, buf);
//                        isFileReceived = true;
//                        ctx.pipeline().addLast(new ServerFileSenderHandler());
                        break;
                    }
                }
            }
        }

        if (buf.readableBytes() == 0) {
            buf.release();
            System.out.println("ServerFileReceiverHandler - buf.release()\n====================");
        }

    }

    private void sendServerFilesList(ChannelHandlerContext ctx, ByteBuf outBuf) {
        serverFilesCount = 0;
        serverFileLength = 0;
        List<byte[]> severFiles = getFilesList();
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


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

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
