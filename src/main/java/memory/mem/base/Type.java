package memory.mem.base;

import java.util.*;

public record Type(String name, String superName, int size, boolean isOop, boolean isInt, boolean isUnsigned, Set<Field> fields) {
    private static final Set<Field> NO_FIELDS = Set.of();
    private static final Map<Type, Set<Field>> fieldsByType = new HashMap<>();

    public Field field(String name) {
        fieldsByType.putIfAbsent(this, fields == null ? NO_FIELDS : Set.copyOf(fields));
        return fieldsByType.get(this).stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow(() -> new NoSuchElementException("No such field: " + name));
    }

    public long global(String name) {
        Field field = field(name);
        if (field.isStatic()) {
            return field.offset();
        }
        throw new IllegalArgumentException("Static field expected");
    }

    public long offset(String name) {
        Field field = field(name);
        if (!field.isStatic()) {
            return field.offset();
        }
        throw new IllegalArgumentException("Instance field expected");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (superName != null) sb.append(" extends ").append(superName);
        sb.append(" @ ").append(size).append('\n');
        for (Field field : fields) {
            sb.append("  ").append(field).append('\n');
        }
        return sb.toString();
    }
}
