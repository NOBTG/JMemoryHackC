package memory.mem.base;

public abstract class CBase {
    private final long ptr;

    public CBase(long ptr) {
        this.ptr = ptr;
    }

    public long getPtr() {
        return ptr;
    }

    static {
        if (!System.getProperty("java.version").equals("17.0.10")) {
            System.err.println("We do not support versions other than JDK 17.0.10");
        }
    }
}
