package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final Connections<T> connections;
    private final int connectionId;
    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private String username;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol, Connections<T> connections, int connectionId) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
        this.connectionId = connectionId;
        this.in = null;
        this.out = null;
        this.username = null;
    }

    @Override
    public void send(T msg) throws IOException {
        byte[] encoded = encdec.encode(msg);
        out.write(encoded);
        out.flush();
        System.out.println("Sent: " + Arrays.toString(encoded));
    }

    @Override
    public void close() throws IOException {
        //System.out.println("Closed connection: " + connectionId);
        if (connections.logout(connectionId))
            connections.removeConnection(connectionId);
        connected = false;
        sock.close();
        
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            protocol.start(connectionId, connections);
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                //System.out.println("Read: " + String.format("%02X ", (byte) read));
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                }
            }
            try { close(); } catch (IOException ignored) { } // For linux connection lost (BAD)
        }
        catch (SocketException ex) {
            try { close(); } catch (IOException ignored) { } // For windows connection lost
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
