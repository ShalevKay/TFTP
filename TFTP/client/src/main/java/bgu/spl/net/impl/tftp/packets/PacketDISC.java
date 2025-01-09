package bgu.spl.net.impl.tftp.packets;

public class PacketDISC extends Packet {

        private static final short opcode = 10;

        public PacketDISC() {
            super(opcode);
        }

        @Override
        public byte[] toBytes() {
            byte[] bytes = new byte[2];
            System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
            return bytes;
        }
}
