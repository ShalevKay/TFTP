package bgu.spl.net.impl.tftp.packets;

public class PacketDATA extends Packet {

    private static final short opcode = 3;
    private final short packetSize;
    private final short blockNumber;
    private final byte[] data;

    public PacketDATA(short blockNumber, byte[] data) {
        super(opcode);

        if (data.length > 512)
            throw new IllegalArgumentException("Data length must be at most 512 bytes");

        if (blockNumber < 0)
            throw new IllegalArgumentException("Block number cannot be negative");

        this.packetSize = (short) data.length;
        this.blockNumber = blockNumber;
        this.data = data;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = new byte[6 + data.length];
        System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
        System.arraycopy(new byte[] {(byte)(packetSize >> 8), (byte)(packetSize & 0xff)}, 0, bytes, 2, 2);
        System.arraycopy(new byte[] {(byte)(blockNumber >> 8), (byte)(blockNumber & 0xff)}, 0, bytes, 4, 2);
        System.arraycopy(data, 0, bytes, 6, data.length);
        return bytes;
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    public byte[] getData() {
        return data;
    }
}
