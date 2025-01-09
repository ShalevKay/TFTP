package bgu.spl.net.impl.tftp.packets;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PacketERROR extends Packet {

    private static final short opcode = 5;
    private static final Map<Short, String> errorCodes = Stream.of(new Object[][] {
        //{ (short)0, "Not defined, see error message (if any)." },
        { (short)1, "File not found." },
        { (short)2, "Access violation." },
        { (short)3, "Disk full or allocation exceeded." },
        { (short)4, "Illegal TFTP operation." },
        { (short)5, "File already exists." },
        { (short)6, "User not logged in." },
        { (short)7, "User already logged in." }
    }).collect(Collectors.toMap(data -> (Short)data[0], data -> (String)data[1]));

    private final short errorCode;
    private final String errorMsg;

    public PacketERROR(short errorCode) {
        super(opcode);

        if (!errorCodes.containsKey(errorCode))
            throw new IllegalArgumentException("Invalid error code");

        this.errorCode = errorCode;
        this.errorMsg = errorCodes.get(errorCode);
    }

    public PacketERROR(short errorCode, String errorMsg) {
        super(opcode);

        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = new byte[4 + errorMsg.length() + 1];
        System.arraycopy(new byte[] {(byte)(opcode >> 8), (byte)(opcode & 0xff)}, 0, bytes, 0, 2);
        System.arraycopy(new byte[] {(byte)(errorCode >> 8), (byte)(errorCode & 0xff)}, 0, bytes, 2, 2);
        System.arraycopy(errorMsg.getBytes(StandardCharsets.UTF_8), 0, bytes, 4, errorMsg.length());
        bytes[errorMsg.length() + 4] = 0;
        return bytes;
    }

    public String getErrorMsg(){
        return errorMsg;
    }

}
