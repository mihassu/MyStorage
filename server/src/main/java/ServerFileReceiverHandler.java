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

public class ServerFileReceiverHandler extends ChannelInboundHandlerAdapter {

    private BufferedOutputStream out;
    private File fileServer;
    private int fileNameLength;
    private long fileSize;
    private long receivedFileSize;

    private int serverFilesCount;
    private int serverFileLength;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        receivedFileSize = 0L;

        //прочитать длину имени файла
        if (buf.readableBytes() >= 4) {
            fileNameLength = buf.readInt();
        }

        //прочитать имя файла и создать файл
        if (buf.readableBytes() >= fileNameLength) {
            byte[] fileName = new byte[fileNameLength];
            buf.readBytes(fileName);
            fileServer = new File("server-storage/" + new String(fileName, StandardCharsets.UTF_8));
            out = new BufferedOutputStream(new FileOutputStream(fileServer));
        }

        //прочитать размер файла
        if (buf.readableBytes() >= 8) {
            fileSize = buf.readLong();
        }

        //прочитать файл
        while (buf.readableBytes() > 0) {
            out.write(buf.readByte());
            receivedFileSize++;
            if (receivedFileSize == fileSize) {
                out.close();
                break;
            }
        }

        if (buf.readableBytes() == 0) {
            buf.release();
            System.out.println("buf.release()");
        }

        List<byte[]> severFiles = getFilesList();
        serverFilesCount = severFiles.size();
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(serverFilesCount);
        ctx.channel().writeAndFlush(buf); //int количество файлов

        for (int i = 0; i < serverFilesCount; i++) {
            serverFileLength = severFiles.get(i).length;
            buf = ByteBufAllocator.DEFAULT.directBuffer(4);
            ctx.channel().writeAndFlush(buf);

            buf = ByteBufAllocator.DEFAULT.directBuffer(serverFileLength);
            buf.writeBytes(severFiles.get(i));
            ctx.channel().writeAndFlush(buf);
        }
        severFiles.forEach(bytes -> {
            ByteBuf bufOut;
            bufOut = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
            ctx.writeAndFlush(bytes.length); //int длина имени файла
            ctx.writeAndFlush(bytes); //byte имя файла
            System.out.println(bytes);
        });
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
