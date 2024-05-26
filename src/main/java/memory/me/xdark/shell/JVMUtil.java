package memory.me.xdark.shell;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public final class JVMUtil {
    public static final Unsafe UNSAFE;
    public static final MethodHandles.Lookup LOOKUP;
    private static final NativeLibraryLoader NATIVE_LIBRARY_LOADER;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = UNSAFE = (Unsafe) field.get(null);
            field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            LOOKUP = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
            NATIVE_LIBRARY_LOADER = new NativeLibraryLoader();
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public static NativeLibrary findJvm() throws Throwable {
        Path jvmDir = Paths.get(System.getProperty("java.home"));
        Path maybeJre = jvmDir.resolve("jre");
        if (Files.isDirectory(maybeJre)) {
            jvmDir = maybeJre;
        }
        jvmDir = jvmDir.resolve("bin");
        String os = System.getProperty("os.name").toLowerCase();
        Path pathToJvm;
        if (os.contains("win")) {
            pathToJvm = findFirstFile(jvmDir, "server/jvm.dll", "client/jvm.dll");
        } else if (os.contains("nix") || os.contains("nux")) {
            pathToJvm = findFirstFile(jvmDir, "lib/amd64/server/libjvm.so", "lib/i386/server/libjvm.so");
        } else {
            throw new RuntimeException("Unsupported OS (probably MacOS X): " + os);
        }
        return NATIVE_LIBRARY_LOADER.loadLibrary(pathToJvm.normalize().toString());
    }

    private static Path findFirstFile(Path directory, String... files) {
        for (String file : files) {
            Path path = directory.resolve(file);
            if (Files.exists(path)) return path;
        }
        throw new RuntimeException("Failed to find one of the required paths!: " + Arrays.toString(files));
    }

    private static final class NativeLibraryLoader {
        private static final MethodHandle CNSTR_NATIVE_LIBRARY;
        private static final MethodHandle MH_NATIVE_LOAD;
        private static final MethodHandle MH_NATIVE_FIND;

        static {
            MethodHandles.Lookup lookup = LOOKUP;
            try {
                Class<?> cl = Class.forName("jdk.internal.loader.NativeLibraries", true, ShellcodeRunner.MyClassLoader.getInstance());
                Class<?> cls = Class.forName("jdk.internal.loader.NativeLibraries$NativeLibraryImpl", true, ShellcodeRunner.MyClassLoader.getInstance());
                MH_NATIVE_LOAD = lookup.findStatic(cl, "load", MethodType.methodType(boolean.class, cls, String.class, boolean.class, boolean.class, boolean.class));
                MH_NATIVE_FIND = lookup.findStatic(cl, "findEntry0", MethodType.methodType(long.class, cls, String.class));
                CNSTR_NATIVE_LIBRARY = lookup.findConstructor(cls, MethodType.methodType(void.class, Class.class, String.class, boolean.class, boolean.class));
            } catch (Throwable t) {
                throw new ExceptionInInitializerError(t);
            }
        }

        NativeLibrary loadLibrary(String path) throws Throwable {
            Object library = CNSTR_NATIVE_LIBRARY.invoke(JVMUtil.class, path, false, false);
            MH_NATIVE_LOAD.invoke(library, path, false, false, true);
            return entry -> {
                try {
                    return (long) MH_NATIVE_FIND.invoke(library, entry);
                } catch (Throwable t) {
                    throw new InternalError(t);
                }
            };
        }
    }
}
