package bgu.spl.net.impl.tftp.packets;

import java.nio.charset.StandardCharsets;

public abstract class Packet {

    protected short opcode;

    public Packet(short opcode) {
        this.opcode = opcode;
    }

    public abstract byte[] toBytes();

    public short getOpcode() {
        return opcode;
    }

    public static Packet bytesToPacket(byte[] message) {
        short opcode = (short)((message[0] << 8) | (message[1] & 0xff));

        String fileName;
        short blockNumber;

        switch (opcode){
            case 1: // RRQ
                fileName = new String(message, 2, message.length - 2 - 1, StandardCharsets.UTF_8);
                return new PacketRRQ(fileName);
            case 2: //WRQ
                fileName = new String(message, 2, message.length - 2 - 1, StandardCharsets.UTF_8);
                return new PacketWRQ(fileName);
            case 3: //DATA
                blockNumber = (short)((message[4] << 8) | (message[5] & 0xff));

                byte[] data = new byte[message.length - 6];
                System.arraycopy(message, 6, data, 0, message.length - 6);

                return new PacketDATA(blockNumber, data);
            case 4: //ACK
                blockNumber = (short)((message[2] << 8) | (message[3] & 0xff));
                return new PacketACK(blockNumber);
            case 5: //ERROR
                short errorCode = (short)((message[2] << 8) | (message[3] & 0xff));
                String errorMsg = new String(message, 4, message.length - 4 - 1, StandardCharsets.UTF_8);
                return new PacketERROR(errorCode, errorMsg);
            case 6: //DIRQ
                return new PacketDIRQ();
            case 7: //LOGRQ
                String userName = new String(message, 2, message.length - 2 - 1, StandardCharsets.UTF_8);
                return new PacketLOGRQ(userName);
            case 8: //DELRQ
                fileName = new String(message, 2, message.length - 2 - 1, StandardCharsets.UTF_8);
                return new PacketDELRQ(fileName);
            case 9: //BCAST
                boolean isAdded = message[2] == 1;
                fileName = new String(message, 3, message.length - 3 - 1, StandardCharsets.UTF_8);
                return new PacketBCAST(fileName, isAdded);
            case 10: //DISC
                return new PacketDISC();
            default:
                throw new IllegalArgumentException("Invalid opcode");
        }
    }

}
