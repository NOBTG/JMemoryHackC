package memory.mem.array;

import memory.mem.CTool;
import memory.mem.jvm.JVM;
import memory.mem.base.CObject;
import memory.mem.base.Type;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Array extends CObject {
    public Array(long ptr, String name) {
        super(ptr);
        CField.ARRAY = JVM.type("Array<".concat(name).concat(">"));
    }

    public enum CField {
        _data;

        private long offset = 0L;
        private static Type ARRAY;

        public long offset() {
            if (offset == 0L) {
                this.offset = ARRAY.offset(this.name());
            }
            return offset;
        }
    }

    public <t> t getField(CField field, Function<Long, t> c) {
        return c.apply(getPtr() + field.offset());
    }

    public <t> void putField(CField field, t obj, BiConsumer<Long, t> c) {
        c.accept(getPtr() + field.offset(), obj);
    }

    public void forEach(Consumer<Long> addr) {
        long data = getField(CField._data, Function.identity());
        for (int i = 0; i < JVM.getInt(getPtr()); i++) {
            addr.accept(JVM.getAddress(data + (long) i * CTool.oopSize));
        }
    }
}
