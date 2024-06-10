package memory.mem.constants;

import memory.mem.jvm.JVM;
import memory.mem.base.CObject;
import memory.mem.base.Type;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ConstantPool extends CObject {
    public enum CField {
        _tags, _cache, _pool_holder, _operands, _resolved_klasses, _major_version, _minor_version, _generic_signature_index, _source_file_name_index, _length;

        private long offset = 0L;

        public long offset() {
            if (offset == 0L) {
                this.offset = JVM.CONSTANT_POOL.offset(this.name());
            }
            return offset;
        }
    }

    public ConstantPool(long ptr) {
        super(ptr);
    }

    public <T> T getField(CField field, Function<Long, T> c) {
        return c.apply(getPtr() + field.offset());
    }

    public <T> void putField(CField field, T obj, BiConsumer<Long, T> c) {
        c.accept(getPtr() + field.offset(), obj);
    }
}
