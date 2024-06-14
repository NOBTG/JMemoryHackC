package memory.mem.testes;

import memory.mem.ClassApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public final class TestA {
    public static class L {
        private static final String a = "L";

        public void a() {
            System.out.println("L");
        }
    }
    public static class M {
        private static final L a = new L();

        public void a() {
            System.out.println("M");
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TestMain.K.print();
        ClassApi.setMethodAccessFlag(TestMain.K.class, "print", Modifier.NATIVE, void.class);
        System.out.println(Modifier.isNative(TestMain.K.class.getDeclaredMethod("print").getModifiers()));
        TestMain.K.print();
        System.out.println("SetMethodAccessFlag.");

        L l = new L();
        l.a();
        ClassApi.redefineObjectClass(l, M.class);
        l.a();
    }
}
