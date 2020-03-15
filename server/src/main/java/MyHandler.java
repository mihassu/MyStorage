import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MyHandler extends ChannelInboundHandlerAdapter {

    private BufferedOutputStream out;
    private File fileServer;
    private int fileNameLength;
    private long fileSize;
    private long receivedFileSize;

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
            fileServer = new File("server-storage/" + new String(fileName, "UTF-8"));
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
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
