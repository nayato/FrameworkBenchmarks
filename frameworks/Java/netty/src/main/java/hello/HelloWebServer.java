package hello;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.File;

public class HelloWebServer {

	static {
		ResourceLeakDetector.setLevel(Level.DISABLED);
	}

	private final int port;

	public HelloWebServer(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		// Configure the server.

		if (Epoll.isAvailable()) {
			doRun(new EpollEventLoopGroup(), EpollServerSocketChannel.class, true);
		} else {
			doRun(new NioEventLoopGroup(), NioServerSocketChannel.class, false);
		}
	}

	private void doRun(EventLoopGroup loupGroup, Class<? extends ServerChannel> serverChannelClass, boolean isNative) throws Exception {
		try {
        // SelfSignedCertificate ssc = new SelfSignedCertificate();
		// System.out.println(ssc.certificate().getAbsolutePath());
		// System.out.println(ssc.privateKey().getAbsolutePath());
		File certFile = new File("~/FrameworkBenchmarks/frameworks/Java/netty/identity.crt");
		System.out.println(certFile.length());
		File keyFile = new File("~/FrameworkBenchmarks/frameworks/Java/netty/identity-dec.key");
		System.out.println(keyFile.length());
        SslContext sslCtx = SslContextBuilder.forServer(certFile, keyFile)//ssc.certificate(), ssc.privateKey())
            .build();

			InetSocketAddress inet = new InetSocketAddress(port);

			ServerBootstrap b = new ServerBootstrap();

			if (isNative) {
				b.option(EpollChannelOption.SO_REUSEPORT, true);
			}

			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.option(ChannelOption.SO_REUSEADDR, true);
			b.group(loupGroup).channel(serverChannelClass);
			b.childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true));
			b.childOption(ChannelOption.SO_REUSEADDR, true);

			b.childHandler(new HelloServerInitializer(loupGroup.next(), null));
			Channel ch = b.bind(inet).sync().channel();

			b.childHandler(new HelloServerInitializer(loupGroup.next(), sslCtx));
			Channel chS = b.bind(new InetSocketAddress(8883)).sync().channel();

			System.out.printf("MQTTd started. Listening on: %s%n", inet.toString());

			ch.closeFuture().sync();
			chS.closeFuture().sync();
		} finally {
			loupGroup.shutdownGracefully().sync();
		}
	}

	public static void main(String[] args) throws Exception {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8113;
		}
		new HelloWebServer(port).run();
	}
}
