package ChatHome.message;

/**
 * @description 心跳包消息
 */
public class PingMessage extends Message {
    @Override
    public int getMessageType() {
        return PingMessage;
    }
}
