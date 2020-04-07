package ru.mihassu.mystorage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import ru.mihassu.mystorage.common.Constants;
import ru.mihassu.mystorage.server.db.DbAuthService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class Server {

    private DbAuthService authService;

    public static void main(String[] args) {
        new Server().runServer();
    }

    public Server() {
        this.authService = new DbAuthService();
    }



    public void runServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            authService.connect();
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Ошибка при подключениие к базе данных: " + e.getMessage());
        }

        createServerDirectory();

        try {
            ChannelFuture future = new ServerBootstrap()
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ServerFileReceiverHandler(authService),
                                    new ObjectDecoder(1024 * 1024, null),
                                    new ServerFileSenderHandler());
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .bind(8189).sync();
            future.channel().closeFuture().sync();

        } catch (Exception e) {
            System.out.println("Server - Ошибка сервера: " + e.getMessage());
        } finally {
            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            authService.disconnect();
            System.out.println("Server - finally()");
        }
    }

    private void createServerDirectory() {
        Path path = Paths.get(Constants.serverDir);
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            System.out.println("Ошибка при создании папки: " + e.getMessage());
        }
    }
}
