package bgu.spl.net.impl.tftp.packets;

import java.nio.charset.StandardCharsets;

public class PacketLOGRQ extends Packet {

        private static final short opcode = 7;
        private final String userName;

        public PacketLOGRQ(String userName) {
            super(opcode);
            this.userName = userName;
        }

        @Override
        public byte[] toBytes() {
            byte[] bytes = new byte[2 + userName.length() + 1];
            System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
            System.arraycopy(userName.getBytes(StandardCharsets.UTF_8), 0, bytes, 2, userName.length());
            bytes[userName.length() + 2] = 0;
            return bytes;
        }

        public String getUsername() {
            return userName;
        }
}
