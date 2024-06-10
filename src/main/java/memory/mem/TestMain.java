package memory.mem;

import memory.mem.array.Array;
import memory.mem.base.Type;
import memory.mem.constants.ConstantPool;
import memory.mem.jvm.JVM;
import memory.mem.klass.InstanceKlass;
import memory.mem.klass.Klass;
import memory.mem.method.ConstMethod;
import memory.mem.method.Method;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

public final class TestMain {
    public static class K {
        public static void print() {
            System.out.println("K");
        }
        public static void print(Object o) {
            System.out.println("K");
        }
    }

    public static void main(final String[] args) throws Throwable {
        //int modifiers = K.class.getDeclaredMethod("print").getModifiers();
        //System.out.println(List.of(Modifier.isStatic(modifiers), Modifier.isPublic(modifiers), Modifier.isProtected(modifiers), Modifier.isPrivate(modifiers)));
        ClassApi.setMethodAccessFlag(K.class, "print", Modifier.PUBLIC, void.class);
int modifiers = K.class.getDeclaredMethod("print").getModifiers();
        System.out.println(List.of(Modifier.isStatic(modifiers), Modifier.isPublic(modifiers), Modifier.isProtected(modifiers), Modifier.isPrivate(modifiers)));

        System.exit(0);

        MethodHandles.Lookup lookup = JVM.lookup;
        String name = "memory/mem/TestMain$K";
        byte[] bytes;
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(name.concat(".class"))) {
            assert is != null;
            ClassNode node = new ClassNode();
            ClassReader cr = new ClassReader(is);
            cr.accept(node, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LdcInsnNode ldc) {
                            method.instructions.set(instruction, new LdcInsnNode("Inject!"));
                    }
                }
            }
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(cw);
            bytes = cw.toByteArray();
        }
        Class<?> clz = Class.forName("jdk.internal.misc.Unsafe");
        ((Class<?>) lookup.findVirtual(clz, "defineClass0", MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class)).invoke(lookup.findStaticVarHandle(clz, "theUnsafe", clz).get(), name, bytes, 0, bytes.length, ClassLoader.getSystemClassLoader(), TestMain.class.getProtectionDomain())).getDeclaredMethod("print").invoke(null);
        new Thread(() -> {
            while (true) {
                K.print();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
