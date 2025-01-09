package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Server;

public class TftpServer {
    public static void main(String[] args) {
        Server.threadPerClient(
                7777, //port
                TftpProtocol::new, //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
        ).serve();
    }
}
