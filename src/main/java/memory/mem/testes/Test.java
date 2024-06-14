package memory.mem.testes;

import memory.mem.CTool;
import memory.mem.array.Array;
import memory.mem.constants.ConstantPool;
import memory.mem.jvm.JVM;
import memory.mem.klass.InstanceKlass;

public final class Test {
    public static class J {
        public static void main(final String[] args) {
            System.out.println("Hello World!");
        }
    }

    public static final byte Long = 5;
    public static final byte Double = 6;

    public static void main(String[] args) {
        ConstantPool con = new ConstantPool(InstanceKlass.of(J.class).getField(InstanceKlass.CField._constants, JVM::getAddress));
        int conSize = JVM.CONSTANT_POOL.size();
        Array array = new Array(con.getField(ConstantPool.CField._tags, JVM::getAddress), "u1");
        for (int i = 0; i < JVM.getInt(array.getPtr()); i++) {
            byte tag = array.get(i);
            if (tag == 1) {
                System.out.println(CTool.getSymbol(con.getPtr() + conSize + (long) i * CTool.oopSize));
            }
        }
        J.main(args);
    }
}
