package memory.mem.array;

import memory.mem.CTool;
import memory.mem.jvm.JVM;
import memory.mem.base.CObject;
import memory.mem.base.Type;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Array extends CObject {
    private final long DATA_OFFSET;

    public Array(long ptr, String name) {
        super(ptr);
        DATA_OFFSET = JVM.type("Array<".concat(name).concat(">")).offset("_data");
    }

    public void forEach(BiConsumer<Long, Integer> addr) {
        for (int i = 0; i < JVM.getInt(getPtr()); i++) {
            addr.accept(JVM.getAddress(DATA_OFFSET + (long) i * CTool.oopSize), i);
        }
    }

    public void forEach(Consumer<Long> addr) {
        forEach((aLong, integer) -> addr.accept(aLong));
    }

    public byte get(int index) {
        return JVM.getByte(getPtr() + DATA_OFFSET + index);
    }

    public void set(int index, byte val) {
        JVM.putByte(getPtr() + DATA_OFFSET + index, val);
    }
}
