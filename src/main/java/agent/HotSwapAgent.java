package agent;

import com.sun.tools.attach.*;
import memory.mem.ClassApi;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class HotSwapAgent {
    private static Instrumentation inst;

    private static void checkAndInitialize() {
        if (inst == null) {
            try {
                Unsafe unsafe;
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    theUnsafe.setAccessible(true);
                    return null;
                });
                unsafe = (Unsafe) theUnsafe.get(null);
                Field allowAttachSelf = Class.forName("sun.tools.attach.HotSpotVirtualMachine").getDeclaredField("ALLOW_ATTACH_SELF");
                Object base = unsafe.staticFieldBase(allowAttachSelf);
                long offset = unsafe.staticFieldOffset(allowAttachSelf);
                if (!unsafe.getBooleanVolatile(base, offset)) {
                    unsafe.putBooleanVolatile(base, offset, Boolean.TRUE);
                }
                VirtualMachineDescriptor vm = VirtualMachine.list().stream().filter(vmd -> vmd.id().equals(String.valueOf(ProcessHandle.current().pid()))).findAny().orElseThrow();
                ClassApi.setMethodAccessFlag(HotSwapAgent.class, "agentmain", Modifier.STATIC | Modifier.PUBLIC, void.class, String.class, Instrumentation.class);
                vm.provider().attachVirtualMachine(vm).loadAgent(createJarWithManifest());
                ClassApi.setMethodAccessFlag(HotSwapAgent.class, "agentmain", Modifier.STATIC | Modifier.PRIVATE, void.class, String.class, Instrumentation.class);
            } catch (NoSuchFieldException | IllegalAccessException | AttachNotSupportedException |
                     ClassNotFoundException | AgentLoadException | AgentInitializationException | IOException e) {
                throw new RuntimeException(e);
            }
            if (!inst.isRetransformClassesSupported() || !inst.isRedefineClassesSupported() || !inst.isNativeMethodPrefixSupported()) {
                throw new AssertionError("Instrumentation State Error!");
            }
        }
    }

    private static void agentmain(String args, Instrumentation inst) {
        HotSwapAgent.inst = inst;
    }

    public static void redefineClasses(ClassFileTransformer transformer, Class<?>... classes) {
        checkAndInitialize();

        if (classes == null || classes.length == 0) {
            throw new RuntimeException("Classes cannot be null or empty!");
        }

        IntStream.range(0, classes.length)
                .filter(i -> classes[i] == null)
                .forEach(i -> {
                    StringBuilder result = new StringBuilder();
                    result.append("[");
                    if (i > 2) {
                        result.append("..., ");
                    }
                    String prefix = IntStream.range(Math.max(0, i - 2), i)
                            .mapToObj(j -> classes[j] != null ? classes[j].getSimpleName() + ".class" : "null")
                            .collect(Collectors.joining(", "));
                    if (!prefix.isEmpty()) {
                        result.append(prefix).append(", ");
                    }
                    result.append("null <- Cannot Null");
                    String suffix = IntStream.range(i + 1, Math.min(classes.length, i + 3))
                            .mapToObj(j -> classes[j] != null ? classes[j].getSimpleName() + ".class" : "null")
                            .collect(Collectors.joining(", "));
                    if (!suffix.isEmpty()) {
                        result.append(", ").append(suffix);
                    }
                    if (i + 3 < classes.length) {
                        result.append(", ...");
                    }
                    result.append("] : index = ").append(i);
                    System.err.println(result);
                });

        List<Class<?>> cantModify = Arrays.stream(classes).filter(c -> !inst.isModifiableClass(c)).toList();
        if (!cantModify.isEmpty()) {
            System.err.println("Can't modify the ".concat(cantModify.size() == 1 ? "class" : "classes").concat(": ").concat(Arrays.toString(cantModify.toArray(Class<?>[]::new))));
            return;
        }

        inst.addTransformer(transformer, true);
        try {
            inst.retransformClasses(classes);
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
        inst.removeTransformer(transformer);
    }

    public static Class<?>[] getAllLoadedClasses() {
        checkAndInitialize();
        return inst.getAllLoadedClasses();
    }

    public static Class<?>[] getInitiatedClasses(ClassLoader loader) {
        checkAndInitialize();
        return inst.getInitiatedClasses(loader);
    }

    public static long getObjectSize(Object objectToSize) {
        checkAndInitialize();
        return inst.getObjectSize(objectToSize);
    }

    public static void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        checkAndInitialize();
        inst.appendToBootstrapClassLoaderSearch(jarfile);
    }

    public static void appendToSystemClassLoaderSearch(JarFile jarfile) {
        checkAndInitialize();
        inst.appendToSystemClassLoaderSearch(jarfile);
    }

    public static synchronized void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        checkAndInitialize();
        inst.setNativeMethodPrefix(transformer, prefix);
    }

    public static void redefineModule(Module module, Set<Module> extraReads, Map<String, Set<Module>> extraExports, Map<String, Set<Module>> extraOpens, Set<Class<?>> extraUses, Map<Class<?>, List<Class<?>>> extraProvides) {
        checkAndInitialize();
        inst.redefineModule(module, extraReads, extraExports, extraOpens, extraUses, extraProvides);
    }

    private static String createJarWithManifest() throws IOException {
        String name = HotSwapAgent.class.getName();
        String classVMName = name.replace('.', '/') + ".class";
        String TRUE = Boolean.toString(Boolean.TRUE);

        Manifest manifest = new Manifest();
        Attributes attr = manifest.getMainAttributes();
        attr.putValue("Manifest-Version", String.valueOf(1.0F));
        attr.putValue("Agent-Class", name);
        attr.putValue("Can-Redefine-Classes", TRUE);
        attr.putValue("Can-Retransform-Classes", TRUE);
        attr.putValue("Can-Set-Native-Method-Prefix", TRUE);

        String path = File.createTempFile(String.valueOf(ThreadLocalRandom.current().nextInt()), ".jar").getAbsolutePath();

        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(path), manifest)) {
            jarOut.putNextEntry(new JarEntry(classVMName));
            try (InputStream is = HotSwapAgent.class.getClassLoader().getResourceAsStream(classVMName)) {
                if (is == null) throw new AssertionError();
                jarOut.write(is.readAllBytes());
            }
            jarOut.closeEntry();
        }

        return path;
    }
}
