import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerFileSenderHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ServerFileSenderHandler - channelRead()");
        String name = (String) msg;
        System.out.println(name);
    }

    private boolean fileExist(String fileName) {
        try {
            return Files.list(Paths.get("server-storage/"))
                    .map(Path::toFile)
                    .map(File::getName)
                    .anyMatch(name -> name.equals(fileName));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}