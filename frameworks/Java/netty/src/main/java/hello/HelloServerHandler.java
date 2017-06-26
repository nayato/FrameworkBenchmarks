package hello;

import java.nio.charset.Charset;
import java.util.concurrent.ScheduledExecutorService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;

public class HelloServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

	private static final Charset utf8 = Charset.forName("UTF-8");
	private static final MqttFixedHeader PubAckHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
	private static final MqttMessage PingMessage = new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0));

	HelloServerHandler(ScheduledExecutorService service) {
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		try
		{
			MqttFixedHeader header = msg.fixedHeader();
			switch (header.messageType())
			{
				case CONNECT:
					ctx.write(new MqttConnAckMessage(new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 2), new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, false)));
					break;
				case PUBLISH:
					MqttPublishMessage publish = (MqttPublishMessage)msg;
					switch (header.qosLevel()) {
						case AT_LEAST_ONCE:
							ctx.write(new MqttPubAckMessage(PubAckHeader, MqttMessageIdVariableHeader.from(publish.variableHeader().packetId())));
							break;
						case AT_MOST_ONCE:
							ctx.write(MqttMessageBuilders.publish().topicName("abc").payload(publish.payload().duplicate().retain()));
							break;
						default:
					}
					break;
				case SUBSCRIBE:
					//MqttSubscribeMessage subscribe = (MqttSubscribeMessage)msg;
					//ctx.write(new MqttSubAckMessage(new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2 + subscribe.payload().topicSubscriptions().size()), MqttMessageIdVariableHeader.from(subscribe.variableHeader().messageId()), new MqttSubAckPayload(grantedQoSLevels)));
					break;
				case UNSUBSCRIBE:
					//context.WriteAsync(UnsubAckPacket.InResponseTo(unsub));
					break;
				case PINGREQ:
					ctx.write(PingMessage);
					break;
				default:
					break;
			}
		}
		catch (Throwable t)
		{
			System.out.println(t.toString());
		}
		finally {
			ReferenceCountUtil.safeRelease(msg);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//System.out.printf(cause.toString());
		ctx.close();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
}
