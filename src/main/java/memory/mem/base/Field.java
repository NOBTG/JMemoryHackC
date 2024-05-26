package memory.mem.base;

public record Field(String name, String typeName, long offset, boolean isStatic) implements Comparable<Field> {
    @Override
    public int compareTo(Field o) {
        if (isStatic != o.isStatic) {
            return isStatic ? -1 : 1;
        }
        return Long.compare(offset, o.offset);
    }

    @Override
    public String toString() {
        if (isStatic) {
            return "static " + typeName + ' ' + name + " @ 0x" + Long.toHexString(offset);
        } else {
            return typeName + ' ' + name + " @ " + offset;
        }
    }
}
