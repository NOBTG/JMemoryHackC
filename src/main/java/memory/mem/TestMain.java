package memory.mem;

import memory.mem.jvm.CodeRunner;
import memory.mem.jvm.JVM;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.Arrays;

public final class TestMain {
    private static class K {
        public static void print() {
            System.out.println("K");
        }
    }
    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[Integer.MAX_VALUE - 8];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    public static void main(final String[] args) throws Throwable {
        MethodHandles.Lookup lookup = JVM.lookup;
        String name = K.class.getName().replace(".", "/");
        byte[] bytes;
        try (InputStream is = K.class.getClassLoader().getResourceAsStream(name.concat(".class"))) {
            assert is != null;
            ClassNode node = new ClassNode();
            ClassReader cr = new ClassReader(is);
            cr.accept(node, 0);
            node.methods.clear();
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(cw);
            bytes = cw.toByteArray();
        }
        Class<?> clz = Class.forName("jdk.internal.misc.Unsafe");
        Class<?> cla = (Class<?>) lookup.findVirtual(clz, "defineClass0", MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class)).invoke(lookup.findStaticVarHandle(clz, "theUnsafe", clz).get(), name, bytes, 0, bytes.length, ClassLoader.getPlatformClassLoader(), TestMain.class.getProtectionDomain());
        new Thread(() -> {
            while (true) {
                System.out.println(Arrays.toString(cla.getDeclaredMethods()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
