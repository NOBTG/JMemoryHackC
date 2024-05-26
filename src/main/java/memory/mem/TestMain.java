package memory.mem;

import memory.mem.array.Array;
import memory.mem.klass.InstanceKlass;
import memory.mem.base.Field;

public final class TestMain {
    private static class K {}

    public static void main(final String[] args) throws NoSuchFieldException {
        JVM jvm = JVM.INSTANCE;
        for (Field method : jvm.type("Method").fields) {
            System.out.println(method);
        }
        System.out.println(System.lineSeparator());
        for (Field method : jvm.type("ConstMethod").fields) {
            System.out.println(method);
        }
        System.out.println(System.lineSeparator());
        for (Field method : jvm.type("ConstantPool").fields) {
            System.out.println(method);
        }
        Array array = new Array(InstanceKlass.of(K.class).getField(InstanceKlass.CField._methods, jvm::getAddress), "Method*");
        System.out.println(array.getField(Array.CField._data, jvm::getAddress));
    }
}
