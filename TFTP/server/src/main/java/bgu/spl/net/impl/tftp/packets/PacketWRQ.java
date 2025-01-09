package bgu.spl.net.impl.tftp.packets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PacketWRQ extends Packet {

    private static final short opcode = 2;
    private final String fileName;

    private FileOutputStream file;

    public PacketWRQ(String fileName) {
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
            File filePath = new File("Files");
            File newfile = new File(filePath, fileName);

            if (!newfile.exists()) {
                this.file = new FileOutputStream(newfile, false);
                return new PacketACK((short) 0);
            }
            else return new PacketERROR((short) 5);
        }
        catch (FileNotFoundException e) {
            return new PacketERROR((short) 1);   
        }
        catch (Exception e) { return new PacketERROR((short) 0, e.toString()); }
    }

    public Packet writeData(byte[] data) {
        if (file == null) return new PacketERROR((short)0, "File not opened yet."); // File not open (WRQ not received yet)
        try { file.write(data); file.flush(); return null; }
        catch (FileSystemException e) {
            if (Pattern.compile(": No space left on device$").matcher(e.getMessage()).find()){
                return new PacketERROR((short) 3);
            }
            return new PacketERROR((short)0, e.getMessage()); 
        }
        catch (IOException e) { return new PacketERROR((short) 0, e.getMessage()); }
    }

    public void closeFile() {
        try { file.close(); }
        catch (IOException ignored) { }
    }

    public String getFileName() {
        return fileName;
    }
}
