package memory.mem.method;

import memory.mem.base.CObject;
import memory.one.helfy.Type;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Method extends CObject {
    public enum CField {
        _constMethod, _method_data, _method_counters, _access_flags,
        _vtable_index, _intrinsic_id, _flags, _i2i_entry, _from_compiled_entry,
        _code, _from_interpreted_entry;

        private long offset = 0L;
        private static final Type METHOD = jvm.type("Method");

        public long offset() {
            if (offset == 0L) {
                this.offset = METHOD.offset(this.name());
            }
            return offset;
        }
    }

    public Method(long ptr) {
        super(ptr);
    }

    public <T> T getField(CField field, Function<Long, T> c) {
        return c.apply(getPtr() + field.offset());
    }

    public <T> void putField(CField field, T obj, BiConsumer<Long, T> c) {
        c.accept(getPtr() + field.offset(), obj);
    }
}
