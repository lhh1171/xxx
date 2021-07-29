package handler;

import com.google.gson.Gson;
import enity.Message;
import enity.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static ConcurrentHashMap<String, ChannelId> channelIdMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, String> nameMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String,String> Checkout = new ConcurrentHashMap<>();
//    public static ConcurrentHashMap<String, ArrayList> friend = new ConcurrentHashMap<>();
//    public static ConcurrentHashMap<SocketAddress,String> addressMap=new ConcurrentHashMap<>();
    public static AtomicInteger online = new AtomicInteger();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame webSocketFrame) throws Exception {

        Message message = new Gson().fromJson(webSocketFrame.text(), Message.class);
        if (message == null) {
            sendMessageByChannel(channelHandlerContext.channel(),
                    new Message(channelHandlerContext.channel().id().asShortText(),
                    "消息错误", System.currentTimeMillis(), MessageType.CHAT_MSG.name()));
            return;
        } else {
            //改名字
            if (MessageType.CHANGE_NAME.name().equals(message.getMessageType())) {
                System.out.println("断点1");
                System.out.println(channelHandlerContext.channel().id().asShortText()+":"+message.getContent());
                if(nameMap.get(message.getContent())!=null) {
                    sendMessageByChannel(channelHandlerContext.channel(),
                            new Message("", "姓名重复请重新输入", new Date().getTime(),
                                    MessageType.CHAT_MSG.name()));
                }else {
                    if(Checkout.get(channelHandlerContext.channel().id().asShortText())==null) {
                        Checkout.put(channelHandlerContext.channel().id().asShortText(), message.getContent());
                        nameMap.put(message.getContent(), channelHandlerContext.channel().id().asShortText());
                    }else{
                        String oldname = Checkout.get(channelHandlerContext.channel().id().asShortText());
                        nameMap.remove(Checkout.get(channelHandlerContext.channel().id().asShortText()));
                        Checkout.remove(oldname);
                    }
                    System.out.println(channelHandlerContext.channel().id().asShortText()
                            + ":" + message.getContent());
                }
            }

            //发送消息
            if(MessageType.CHAT_MSG.name().equals(message.getMessageType())) {
                //aaa1
                if(nameMap.get(message.getId())!=null){
                String str =nameMap.get(message.getId());
                    System.out.println(str);
                ChannelId channelId = channelIdMap.get(str);
                    System.out.println(channelId);
                if (channelId == null) {
                    sendMessageByChannel(channelHandlerContext.channel(),
                            new Message(channelHandlerContext.channel().id().asShortText(),
                                    "对方已下线", System.currentTimeMillis(), MessageType.CHAT_MSG.name()));
                } else {
                    sendMessageByChannel(channelGroup.find(channelId), message);
                }
                }else{
                    sendMessageByChannel(channelHandlerContext.channel(),
                            new Message(channelHandlerContext.channel().id().asShortText(),
                                    "查无此人",System.currentTimeMillis(),MessageType.CHAT_MSG.name()));
                }
            }

            //添加好友
            if(MessageType.USER_ADD.name().equals(message.getMessageType())){
                String msg = message.getContent();
                if(nameMap.get(msg) == null){
                    System.out.println("aaaaaaaa");
                    sendMessageByChannel(channelHandlerContext.channel(),
                            new Message(channelHandlerContext.channel().id().asShortText(),
                                    "未找到此人",System.currentTimeMillis(),MessageType.CHAT_MSG.name()));
                }else{
                    System.out.println("BBBBBBBBBB");
                    sendMessageByChannel(channelHandlerContext.channel(),
                            new Message(channelHandlerContext.channel().id().asShortText(),
                                    "好友"+msg,System.currentTimeMillis(),MessageType.USER_ADD.name()));
                }
            }
        }
        System.out.println(channelHandlerContext.channel().remoteAddress() + "--->" +
                message.getContent() + "--->" + message.getTimestamp());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        channelGroup.add(ctx.channel());
        channelIdMap.put(ctx.channel().id().asShortText(), ctx.channel().id());
        online.set(channelGroup.size());
        sendMessageForAll(new Message("", ctx.channel().id().asShortText(),
                System.currentTimeMillis(), MessageType.USER_ADD.name()));
        System.out.println(ctx.channel().remoteAddress() + "上线！" + "--->" + ctx.channel().id().asShortText());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        channelGroup.remove(ctx.channel());
        channelIdMap.remove(ctx.channel().id().asShortText());
        online.set(channelGroup.size());
        sendMessageForAll(new Message("", ctx.channel().id().asShortText(),
                System.currentTimeMillis(), MessageType.USER_LEAVE.name()));
        System.out.println(ctx.channel().remoteAddress() + "下线！");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
    
    private void sendMessageByChannel(Channel channel, Message message) {
        channel.writeAndFlush(new TextWebSocketFrame(new Gson().toJson(message)));
        //gson ==>json
    }

    private void sendMessageForAll(Message message) {
        for (Channel channel : channelGroup) {
            channel.writeAndFlush(new TextWebSocketFrame(new Gson().toJson(message)));
        }
    }
}
