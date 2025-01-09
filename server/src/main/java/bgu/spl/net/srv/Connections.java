package bgu.spl.net.srv;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);

    void sendAll(T msg);

    boolean login(int connectionId, String username);
    boolean logout(int connectionId);
    boolean isLoggedIn(int connectionId);
    void removeConnection(int connectionId);

    boolean isFileInUse(String filepath);

    void setFileInUse(String filepath, boolean status);
}
