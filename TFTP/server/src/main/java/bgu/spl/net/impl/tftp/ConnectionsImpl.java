package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConnectionsImpl<T> implements Connections<T> {

    private final Map<Integer, ConnectionHandler<T>> connections;
    private final Map<String, Boolean> filesInUse;

    public ConnectionsImpl() {
        this.connections = new HashMap<>();
        this.filesInUse = new HashMap<>();
    }

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        connections.put(connectionId, handler);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        try { connections.get(connectionId).send(msg); }
        catch (IOException e) { return false; }

        return true;
    }

    @Override
    public void disconnect(int connectionId) {
        try { connections.get(connectionId).close(); }
        catch (Exception ignored) { }
    }

    @Override
    public void sendAll(T msg) {
        for (ConnectionHandler<T> handler : connections.values()) {
            try { handler.send(msg); }
            catch (IOException e) {
                System.out.println("Could not send message to: " + handler.getUsername());
            }
        }
    }

    @Override
    public boolean login(int connectionId, String username) {
        for (ConnectionHandler<T> handler : connections.values()) {
            if (handler.getUsername() != null && handler.getUsername().equals(username)) return false;
        }
        connections.get(connectionId).setUsername(username);
        return true;
    }

    @Override
    public boolean logout(int connectionId) {
        if (connections.get(connectionId) != null && connections.get(connectionId).getUsername() != null) {
            connections.get(connectionId).setUsername(null);
            return true;
        }
        return false;
    }

    @Override
    public void removeConnection(int connectionId) {
        connections.remove(connectionId);
    }

    @Override
    public boolean isLoggedIn(int connectionId) {
        return connections.get(connectionId).getUsername() != null;
    }

    public boolean isFileInUse(String filepath) {
        if (filesInUse.containsKey(filepath))
            return filesInUse.get(filepath);
        return false;
    }

    public void setFileInUse(String filepath, boolean status) {
        filesInUse.put(filepath, status);
    }

}
