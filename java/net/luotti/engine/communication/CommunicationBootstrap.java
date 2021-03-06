package net.luotti.engine.communication;

import com.luotti.engine.Environment;
import com.luotti.engine.settings.Properties;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.ResourceLeakDetector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.PlatformDependent;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import net.luotti.engine.communication.sessions.SessionController;

public class CommunicationBootstrap {

    private static int CHANNEL_MEMORY_BASE;
    private static int CHANNEL_MEMORY_LIMIT;

    public static final byte BOSS_POOL_SIZE = 0x01;
    public static final byte WORKER_POOL_SIZE = 0x04;

    static {
        CHANNEL_MEMORY_BASE = CommunicationController.CHANNEL_MEMORY_BASE;
        CHANNEL_MEMORY_LIMIT = CommunicationController.CHANNEL_MEMORY_LIMIT;
    }

    public static boolean bootstrap()
    {
        try
        {
            // TODO: Remove when no leaks are detected!
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

            if (PlatformDependent.isWindows())
            {
                CommunicationController.BOOTSTRAP = new ServerBootstrap().group(
                    new NioEventLoopGroup(CommunicationBootstrap.BOSS_POOL_SIZE),
                    new NioEventLoopGroup(CommunicationBootstrap.WORKER_POOL_SIZE)
                );

                CommunicationController.BOOTSTRAP.channel(NioServerSocketChannel.class);
            }

            else
            {
                CommunicationController.BOOTSTRAP = new ServerBootstrap().group(
                    new EpollEventLoopGroup(CommunicationBootstrap.BOSS_POOL_SIZE),
                    new EpollEventLoopGroup(CommunicationBootstrap.WORKER_POOL_SIZE)
                );

                CommunicationController.BOOTSTRAP.channel(EpollServerSocketChannel.class);
            }

            CommunicationController.BOOTSTRAP.childHandler(Environment.getCommunication());

            CommunicationController.BOOTSTRAP.option(ChannelOption.SO_REUSEADDR, true);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.SO_LINGER, -1);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.SO_RCVBUF, 1024);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.SO_SNDBUF, 4096);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.TCP_NODELAY, true);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.SO_KEEPALIVE, true);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK , CHANNEL_MEMORY_BASE);
            CommunicationController.BOOTSTRAP.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, CHANNEL_MEMORY_LIMIT);

            SessionController.CHANNELS.bind(CommunicationController.BOOTSTRAP.bind(Properties.NIO_GAME_PORT).sync().channel());
        }

        catch (Exception ex) { return false; }  finally { return true; }
    }
}