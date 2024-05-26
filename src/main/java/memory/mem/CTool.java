package memory.mem;

import memory.mem.base.Type;
import memory.mem.jvm.JVM;
import sun.misc.Unsafe;

import java.nio.charset.StandardCharsets;

public final class CTool {
    public static final int oopSize = JVM.intConstant("oopSize");
    private static final long LONG_MASK = 0xffffffffL;
    private static final long klassOffset = JVM.getInt(JVM.type("java_lang_Class").global("_klass_offset"));
    public static final boolean is64BitVM = oopSize == 8;

    public static String getSymbol(long symbolAddress) {
        Type symbolType = JVM.type("Symbol");
        long symbol = JVM.getAddress(symbolAddress);
        long body = symbol + symbolType.offset("_body");
        int length = JVM.getShort(symbol + symbolType.offset("_length")) & 0xffff;

        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = JVM.getByte(body + i);
        }
        return new String(b, StandardCharsets.UTF_8);
    }

    public static void setSymbol(long symbolAddress, String s) {
        Type symbolType = JVM.type("Symbol");
        long symbol = JVM.getAddress(symbolAddress);
        long body = symbol + symbolType.offset("_body");

        JVM.putShort(symbol + symbolType.offset("_length"), (short) s.length());

        byte[] b = s.getBytes();
        for (int j = 0, k = b.length; j < k; j++) {
            JVM.putByte(body + j, b[j]);
        }
    }

    public static long getPtr(Object obj) {
        Unsafe unsafe = JVM.unsafe;
        if (CTool.is64BitVM) {
            return unsafe.getLong(obj, klassOffset);
        } else {
            return unsafe.getInt(obj, klassOffset) & LONG_MASK;
        }
    }
}
