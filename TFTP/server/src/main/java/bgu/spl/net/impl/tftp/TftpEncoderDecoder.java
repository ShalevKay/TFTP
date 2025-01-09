package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private short opcode;
    private short data_size;
    byte[] message = new byte[1 << 9]; // 512
    int len = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {

        pushByte(nextByte);

        // Determine the opcode
        if (len == 2) {
            opcode = (short)((message[0] << 8) | (message[1] & 0xff));
        }

        // Check if the message is complete (ACK, DIRQ, DISC) - Fixed size packets
        if (len == getPacketMinimalSize() && (opcode == 4 || opcode == 6 || opcode == 10))
            return getMessage();

        // For non-fixed size packets
        else if (len >= getPacketMinimalSize()) {

            // Check if the message is complete (RRQ, WRQ, ERROR, LOGRQ, DELRQ, BCAST) - 0 terminated strings
            if (nextByte == 0 && (opcode == 1 || opcode == 2 || opcode == 5 || opcode == 7 || opcode == 8 || opcode == 9))
                return getMessage();

            // For DATA packets
            else if (opcode == 3) {

                // Determine the size of the data
                if (len == 6) {
                    data_size = (short) ((message[2] << 8) | (message[3] & 0xff));
                    //System.out.println("Data size: " + data_size);
                }

                // Check if the message is complete (DATA)
                if (len == 6 + data_size)
                    return getMessage();
            }
            else
                return null;
        }

        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private void pushByte(byte nextByte) {
        if (len >= message.length) {
            message = java.util.Arrays.copyOf(message, len * 2);
        }

        message[len++] = nextByte;
    }

    private byte[] getMessage() {
        byte[] result = java.util.Arrays.copyOf(message, len);
        len = 0;
        return result;
    }

    private int getPacketMinimalSize() {
        switch (opcode) {
            case 1:
            case 2:
            case 7:
            case 8:
                return 3;
            case 3:
                return 6;
            case 4:
            case 9:
                return 4;
            case 5:
                return 5;
            case 6:
            case 10:
                return 2;
            default:
                return -1;
        }
    }
}