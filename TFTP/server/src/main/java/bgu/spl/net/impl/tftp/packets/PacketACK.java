package bgu.spl.net.impl.tftp.packets;

public class PacketACK extends Packet {
    private static final short opcode = 4;
    private final short blockNumber;

    public PacketACK(short blockNumber) {
        super(opcode);

        if (blockNumber < 0) {
            throw new IllegalArgumentException("Block number cannot be negative");
        }

        this.blockNumber = blockNumber;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = new byte[4];
        System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
        System.arraycopy(new byte[] {(byte)(blockNumber >> 8), (byte)(blockNumber & 0xff)}, 0, bytes, 2, 2);
        return bytes;
    }

    public short getBlockNumber() {
        return blockNumber;
    }
}
