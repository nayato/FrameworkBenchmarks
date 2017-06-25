package hello;

import java.util.concurrent.ScheduledExecutorService;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.*;
// import io.netty.handler.codec.http.HttpRequestDecoder;
// import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

public class HelloServerInitializer extends ChannelInitializer<SocketChannel> {

	private ScheduledExecutorService service;
    private final SslContext sslCtx;

	public HelloServerInitializer(ScheduledExecutorService service, SslContext sslCtx) {
		this.service = service;
        this.sslCtx = sslCtx;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
       	if (sslCtx != null) {
		    p.addLast("tls", sslCtx.newHandler(ch.alloc()));
		}
		p.addLast("encoder", MqttEncoder.INSTANCE);
		p.addLast("decoder", new MqttDecoder(256*1024));
		p.addLast("handler", new HelloServerHandler(service));
	}
}
