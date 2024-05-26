package memory.mem.method;

import memory.mem.jvm.JVM;
import memory.mem.base.CObject;
import memory.mem.base.Type;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ConstMethod extends CObject {
    public enum CField {
        _fingerprint, _constants, _stackmap_data, _constMethod_size, _flags, _code_size, _name_index, _signature_index, _method_idnum, _max_stack, _max_locals, _size_of_parameters;

        private long offset = 0L;
        private static final Type CONST_METHOD = JVM.type("ConstMethod");

        public long offset() {
            if (offset == 0L) {
                this.offset = CONST_METHOD.offset(this.name());
            }
            return offset;
        }
    }

    public ConstMethod(long ptr) {
        super(ptr);
    }

    public <T> T getField(CField field, Function<Long, T> c) {
        return c.apply(getPtr() + field.offset());
    }

    public <T> void putField(CField field, T obj, BiConsumer<Long, T> c) {
        c.accept(getPtr() + field.offset(), obj);
    }
}
