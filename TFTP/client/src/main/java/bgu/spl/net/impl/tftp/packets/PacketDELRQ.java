package bgu.spl.net.impl.tftp.packets;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class PacketDELRQ extends Packet {

        private static final short opcode = 8;
        private final String fileName;

        public PacketDELRQ(String fileName) {
            super(opcode);
            this.fileName = fileName;
        }

        @Override
        public byte[] toBytes() {
            byte[] bytes = new byte[2 + fileName.length() + 1];
            System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
            System.arraycopy(fileName.getBytes(StandardCharsets.UTF_8), 0, bytes, 2, fileName.length());
            bytes[fileName.length() + 2] = 0;
            return bytes;
        }

        public boolean deleteFile(){
            File file = new File(fileName);

            return file.delete();
        }

        public String getFileName() {
            return fileName;
        }
}
