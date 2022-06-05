package ru.geekbrain.ChatServer.core;


import ru.geekbrain.ChatCommon.Messages;
import ru.geekbrain.Network.SocketThread;
import ru.geekbrain.Network.SocketThreadListener;


import java.net.Socket;

public class ClientThread extends SocketThread {
    private String nickname;
    private boolean isAuthorized;
    private boolean isReconnecting;

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public boolean isReconnecting() {
        return isReconnecting;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    void reconnect() {
        isReconnecting = true;
        close();
    }

    void authAccept(String nickname) {
        isAuthorized = true;
        this.nickname = nickname;
        sendMessage(Messages.getAuthAccept(nickname));
    }

    void authFail() {
        sendMessage(Messages.getAuthDenied());
        close();
    }

    void msgFormatError(String msg) {
        sendMessage(Messages.getMsgFormatError(msg));
        close();
    }
}