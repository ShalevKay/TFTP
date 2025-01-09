package bgu.spl.net.impl.tftp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Listening implements Runnable {

    private final Socket sock;
    private final TftpEncoderDecoder encdec;
    private final TftpProtocol protocol;

    public Listening(Socket sock, TftpEncoderDecoder encdec, TftpProtocol protocol) {
        this.sock = sock;
        this.encdec = encdec;
        this.protocol = protocol;
    }

    @Override
    public void run() {

        // Read from the server
        int read;
        try (Socket socket = this.sock;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // Read a byte at a time
            while (!protocol.shouldTerminate() && (read = in.read()) >= 0) {

                // Decode the next byte
                byte[] nextMessage = encdec.decodeNextByte((byte) read);

                // If the message is ready, process it
                if (nextMessage != null) {

                    // Process the message
                    byte[] response = protocol.process(nextMessage);

                    // Send the response
                    if (response != null) {
                        out.write(response);
                        out.flush();
                    }
                }
            }
        } catch (IOException ignored) { }
    }


}
