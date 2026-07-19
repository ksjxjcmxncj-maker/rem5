package nro.models.network;

import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;

public final class Sender implements Runnable {

    @NonNull
    private ISession session;
    @NonNull
    private BlockingDeque<Message> messages;
    private DataOutputStream dos;
    private IMessageSendCollect sendCollect;

    public Sender(@NonNull ISession session, @NonNull Socket socket) {
        if (session == null) {
            throw new NullPointerException("session is marked non-null but is null");
        }
        if (socket == null) {
            throw new NullPointerException("socket is marked non-null but is null");
        }
        try {
            this.session = session;
            this.messages = new LinkedBlockingDeque<Message>();
            this.setSocket(socket);
        } catch (Exception exception) {
        }
    }

    public Sender setSocket(@NonNull Socket socket) {
        if (socket == null) {
            throw new NullPointerException("socket is marked non-null but is null");
        }
        try {
            // BufferedOutputStream 64KB: gộp nhiều writeByte/writeShort/write() thành
            // 1 syscall duy nhất khi flush() → giảm đáng kể overhead cho packet nhỏ
            this.dos = new DataOutputStream(
                new BufferedOutputStream(socket.getOutputStream(), 65536)
            );
        } catch (IOException iOException) {
        }
        return this;
    }

    @Override
    public void run() {
        try {
            while (this.session.isConnected()) {
                // Blocking wait tối đa 100ms — không spin, không sleep() lãng phí CPU
                Message message = this.messages.poll(100L, TimeUnit.MILLISECONDS);
                if (message == null) continue;

                // Gửi packet đầu tiên ngay lập tức
                this.doSendMessage(message);
                message.cleanup();

                // Drain burst: nếu có nhiều packets đang chờ (vd chuyển map),
                // gửi tất cả liên tiếp không có delay giữa — giảm lag spike khi burst
                Message next;
                while ((next = this.messages.poll()) != null) {
                    this.doSendMessage(next);
                    next.cleanup();
                }
            }
        } catch (Exception exception) {
        }
    }

    public synchronized void doSendMessage(Message message) throws Exception {
        this.sendCollect.doSendMessage(this.session, this.dos, message);
    }

    public void sendMessage(Message msg) {
        try {
            if (this.session.isConnected()) {
                this.messages.add(msg);
            }
        } catch (Exception exception) {
        }
    }

    public void setSend(IMessageSendCollect sendCollect) {
        this.sendCollect = sendCollect;
    }

    public int getNumMessages() {
        return this.messages.size();
    }

    public void close() {
        this.messages.clear();
        if (this.dos != null) {
            try {
                this.dos.close();
            } catch (IOException iOException) {
            }
        }
    }

    public void dispose() {
        this.session = null;
        this.messages = null;
        this.sendCollect = null;
        this.dos = null;
    }
}
