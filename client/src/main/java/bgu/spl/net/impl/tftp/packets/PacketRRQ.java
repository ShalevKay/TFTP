package bgu.spl.net.impl.tftp.packets;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class PacketRRQ extends Packet {

    private static final short opcode = 1;
    private final String fileName;

    private FileInputStream file;

    public PacketRRQ(String fileName) {
        super(opcode);
        this.fileName = fileName;
        this.file = null;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = new byte[2 + fileName.length() + 1];
        System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
        System.arraycopy(fileName.getBytes(StandardCharsets.UTF_8), 0, bytes, 2, fileName.length());
        bytes[fileName.length() + 2] = 0;
        return bytes;
    }

    public Packet openFile() {
        try {
            File newfile = new File(fileName);

            if (newfile.exists()) {
                this.file = new FileInputStream(newfile);
                return new PacketACK((short) 0);
            } else return new PacketERROR((short) 1);
        }
        catch (Exception e) {
            return new PacketERROR((short) 0, e.getMessage());
        }
    }

    public byte[] readData() {
        if (file == null) return null; // File not open (RRQ not received yet)
        try {
            byte[] data = new byte[512];
            int read = file.read(data, 0, 512);
            if (read == -1) return new byte[0]; // End of file

            // Minimize the array to the actual size
            byte[] result = new byte[read];
            System.arraycopy(data, 0, result, 0, read);

            return result;
        }
        catch (Exception e) { return null; }
    }

    public void closeFile() {
        try { file.close(); }
        catch (Exception ignored) { }
    }

    public String getFileName() {
        return fileName;
    }
}
