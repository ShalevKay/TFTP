package bgu.spl.net.impl.tftp.packets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PacketDIRQ extends Packet {

    private static final short opcode = 6;

    private List<Byte> filesBytes;
    private int lastIndex;

    public PacketDIRQ() {
        super(opcode);
        this.filesBytes = new ArrayList<>();
        this.lastIndex = 0;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = new byte[2];
        System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
        return bytes;
    }

    public void acquireFiles() {
        File filePath = new File("Flies");
        String[] filesStr = filePath.list();
        if (filesStr != null)
        {
            for (String file : filesStr) {
                byte[] bFile = file.getBytes();
                for (byte b : bFile) {
                    filesBytes.add(b);
                }
                filesBytes.add((byte)0);
            }
            if (!filesBytes.isEmpty()){
                int index = filesBytes.size() - 1;
                filesBytes.remove(index);
            }
        }
    }

    public byte[] getNextFileBytes() {
        if(!filesBytes.isEmpty()){
            int newLastIndex = Math.min(lastIndex+512, filesBytes.size());
            List<Byte> nextDataPacket = filesBytes.subList(lastIndex, newLastIndex);
            lastIndex = newLastIndex;
            byte[] result = new byte[nextDataPacket.size()];
            for (int i = 0; i < nextDataPacket.size(); i++) {
                result[i] = nextDataPacket.get(i);
            }

            return result;
        }
        return new byte[0];
    }

    public void addBytes(byte[] bytes) {
        for (byte b : bytes) {
            filesBytes.add(b);
        }
    }

    public byte[] getAllBytes() {
        byte[] result = new byte[filesBytes.size()];
        for (int i = 0; i < filesBytes.size(); i++) {
            result[i] = filesBytes.get(i);
        }
        return result;
    }
}
