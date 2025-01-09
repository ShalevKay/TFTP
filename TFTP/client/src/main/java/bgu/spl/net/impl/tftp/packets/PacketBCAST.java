package bgu.spl.net.impl.tftp.packets;

import java.nio.charset.StandardCharsets;

public class PacketBCAST extends Packet {

        private static final short opcode = 9;
        private final String fileName;
        private final boolean isAdded;

        public PacketBCAST(String fileName, boolean isAdded) {
            super(opcode);
            this.fileName = fileName;
            this.isAdded = isAdded;
        }

        @Override
        public byte[] toBytes() {
            byte[] bytes = new byte[3 + fileName.length() + 1];
            System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
            bytes[2] = (byte)(isAdded ? 1 : 0);
            System.arraycopy(fileName.getBytes(StandardCharsets.UTF_8), 0, bytes, 3, fileName.length());
            bytes[fileName.length() + 3] = 0;
            return bytes;
        }

        public String getFileName(){
            return fileName;
        }

        public String addedOrDeleted(){
            return isAdded ? "add" : "delete";
        }

}
