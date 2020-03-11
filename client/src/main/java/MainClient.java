import java.io.*;

public class MainClient {
    public static void main(String[] args) {
        File fileClient = new File("client-storage/123.txt");
        File fileServer;

        try(FileInputStream finClient = new FileInputStream(fileClient);
            RandomAccessFile out = new RandomAccessFile(new File("server-storage/stream.txt"), "rw");
        ) {
            //отправить в поток длину имени
            out.writeInt(fileClient.getName().length());
            //отправить в поток имя
            out.writeChars(fileClient.getName());

            //как определить размер файла?

            //отправить в поток содержимое
            int b;
            while (true) {
                b = finClient.read();
                if (b == -1) break;
                out.writeByte(b);
            }

            //читаю из того же потока, пока без сервера
            out.seek(0);
            //Прочитать длину имени
            int len = out.readInt();
            //Прочитать имя файла
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                sb.append(out.readChar());
            }
            //создать файл
            fileServer = new File("server-storage/" + sb);

            //записать содержимое файла
            RandomAccessFile in = new RandomAccessFile(fileServer, "rw");
            int biteIn;
            while (true) {
                biteIn = out.read();
                if (biteIn == -1) break;
                in.writeByte(biteIn);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
