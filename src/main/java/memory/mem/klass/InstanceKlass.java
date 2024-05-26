package memory.mem.klass;

import memory.mem.CTool;
import memory.mem.base.CObject;
import memory.one.helfy.Type;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class InstanceKlass extends CObject {
    public enum CField {
        _annotations, _array_klasses, _constants, _inner_classes, _source_debug_extension, _nonstatic_field_size, _static_field_size, _nonstatic_oop_map_size, _itable_len, _static_oop_field_count, _java_fields_count, _idnum_allocated_count, _is_marked_dependent, _init_state, _reference_type, _misc_flags, _init_thread, _oop_map_cache, _jni_ids, _methods_jmethod_ids, _osr_nmethods_head, _breakpoints, _methods, _default_methods, _local_interfaces, _transitive_interfaces, _method_ordering, _default_vtable_indices, _fields;

        private long offset = 0L;
        private static final Type INSTANCE_KLASS = jvm.type("InstanceKlass");

        public long offset() {
            if (offset == 0L) {
                this.offset = INSTANCE_KLASS.offset(this.name());
            }
            return offset;
        }
    }

    public InstanceKlass(long ptr) {
        super(ptr);
    }

    public <T> T getField(CField field, Function<Long, T> c) {
        return c.apply(getPtr() + field.offset());
    }

    public <T> void putField(CField field, T obj, BiConsumer<Long, T> c) {
        c.accept(getPtr() + field.offset(), obj);
    }

    public static InstanceKlass of(Class<?> obj) {
        return new InstanceKlass(CTool.getPtr(obj));
    }
}
