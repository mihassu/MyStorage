import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainClient {

    private static Logger logger = Logger.getLogger(MainClient.class.getName());

    public static void main(String[] args) {

        final CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        try {
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Path path = Paths.get("client-storage/123.txt");
        try {
            FileSender.sendFile(path, Network.getInstance().getChannel(), channelFuture -> {
                if (channelFuture.isSuccess()) {
                    logIt("Файл передан");
                } else {
                    channelFuture.cause().printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void logIt(String logText) {
        logger.log(Level.SEVERE, logText);
    }
}
