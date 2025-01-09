package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;

public class TftpClient {

    // args[0] = host, args[1] = port
    // Connects to the server and starts the listening and keyboard threads
    // The listening thread listens to the server and the keyboard thread listens to the user
    // The threads are terminated when the protocol should terminate
    public static void main(String[] args) throws IOException {

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        // Connect to the server
        try (Socket sock = new Socket(host, port)) {

            System.out.println("Connected to the server");

            // Create the encoder-decoder and the protocol
            TftpEncoderDecoder encdec = new TftpEncoderDecoder();
            TftpProtocol protocol = new TftpProtocol();

            // Create the listening and keyboard threads
            Listening listening = new Listening(sock, encdec, protocol);
            Keyboard keyboard = new Keyboard(sock, protocol);

            Thread listeningThread = new Thread(listening);
            Thread keyboardThread = new Thread(keyboard);

            listeningThread.start();
            keyboardThread.start();

            // Wait for the protocol to terminate
            while (!protocol.shouldTerminate()) {
                // Do nothing
            }
            try {
                listeningThread.join();
                keyboardThread.join();
            } catch (InterruptedException ignored) { }
        }
    }
}
