package memory.mem.klass;

import memory.mem.CTool;
import memory.mem.base.CObject;
import memory.one.helfy.Type;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Klass extends CObject {
    public enum CField {
        _layout_helper, _modifier_flags, _super_check_offset, _name, _secondary_super_cache, _secondary_supers, _primary_supers("_primary_supers[0]"), _java_mirror, _super, _subklass, _next_sibling, _next_link, _class_loader_data, _vtable_len, _access_flags, _prototype_header;

        private Optional<String> name = Optional.empty();
        private long offset = 0L;
        private static final Type KLASS = jvm.type("Klass");

        CField(String name) {
            this.name = Optional.of(name);
        }

        CField() {
        }

        public String getName() {
            return name.orElse(this.name());
        }

        public long offset() {
            if (offset == 0L) {
                this.offset = KLASS.offset(this.getName());
            }
            return offset;
        }
    }

    public Klass(long ptr) {
        super(ptr);
    }

    public <T> T getField(CField field, Function<Long, T> c) {
        return c.apply(getPtr() + field.offset());
    }

    public <T> void putField(CField field, T obj, BiConsumer<Long, T> c) {
        c.accept(getPtr() + field.offset(), obj);
    }

    public static Klass of(Class<?> obj) {
        return new Klass(CTool.getPtr(obj));
    }
}
