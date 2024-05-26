package memory.mem.jvm;

import memory.mem.base.Field;
import memory.mem.base.Type;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

public final class JVM {
    public static final Unsafe unsafe;
    private static final NativeLibrary JVM;
    private static final Map<String, Type> types = new LinkedHashMap<>();
    private static final Map<String, Number> constants = new LinkedHashMap<>();
    public static final MethodHandles.Lookup lookup;

    static {
        try {
            java.lang.reflect.Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            lookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

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

        try {
            MethodHandle CNSTR_NATIVE_LIBRARY;
            MethodHandle MH_NATIVE_LOAD;
            MethodHandle MH_NATIVE_FIND;
            try {
                Class<?> cl = Class.forName("jdk.internal.loader.NativeLibraries");
                Class<?> cls = Class.forName("jdk.internal.loader.NativeLibraries$NativeLibraryImpl");
                MH_NATIVE_LOAD = lookup.findStatic(cl, "load", MethodType.methodType(boolean.class, cls, String.class, boolean.class, boolean.class, boolean.class));
                MH_NATIVE_FIND = lookup.findStatic(cl, "findEntry0", MethodType.methodType(long.class, cls, String.class));
                CNSTR_NATIVE_LIBRARY = lookup.findConstructor(cls, MethodType.methodType(void.class, Class.class, String.class, boolean.class, boolean.class));
            } catch (Throwable t) {
                throw new ExceptionInInitializerError(t);
            }
            String path = pathToJvm.normalize().toString();
            Object library = CNSTR_NATIVE_LIBRARY.invoke(JVM.class, path, false, false);
            MH_NATIVE_LOAD.invoke(library, path, false, false, true);
            JVM = entry -> {
                try {
                    return (long) MH_NATIVE_FIND.invoke(library, entry);
                } catch (Throwable t) {
                    throw new InternalError(t);
                }
            };
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        Map<String, Set<Field>> structs = new HashMap<>();

        long entry = getSymbol("gHotSpotVMStructs");
        long typeNameOffset = getSymbol("gHotSpotVMStructEntryTypeNameOffset");
        long fieldNameOffset = getSymbol("gHotSpotVMStructEntryFieldNameOffset");
        long typeStringOffset = getSymbol("gHotSpotVMStructEntryTypeStringOffset");
        long isStaticOffset = getSymbol("gHotSpotVMStructEntryIsStaticOffset");
        long offsetOffset = getSymbol("gHotSpotVMStructEntryOffsetOffset");
        long addressOffset = getSymbol("gHotSpotVMStructEntryAddressOffset");
        long arrayStride = getSymbol("gHotSpotVMStructEntryArrayStride");

        for (; ; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            String fieldName = getStringRef(entry + fieldNameOffset);
            if (fieldName == null) break;

            String typeString = getStringRef(entry + typeStringOffset);
            boolean isStatic = getInt(entry + isStaticOffset) != 0;
            long offset = getLong(entry + (isStatic ? addressOffset : offsetOffset));

            Set<Field> fields = structs.computeIfAbsent(typeName, k -> new TreeSet<>());
            fields.add(new Field(fieldName, typeString, offset, isStatic));
        }

        entry = getSymbol("gHotSpotVMTypes");
        typeNameOffset = getSymbol("gHotSpotVMTypeEntryTypeNameOffset");
        long superclassNameOffset = getSymbol("gHotSpotVMTypeEntrySuperclassNameOffset");
        long isOopTypeOffset = getSymbol("gHotSpotVMTypeEntryIsOopTypeOffset");
        long isIntegerTypeOffset = getSymbol("gHotSpotVMTypeEntryIsIntegerTypeOffset");
        long isUnsignedOffset = getSymbol("gHotSpotVMTypeEntryIsUnsignedOffset");
        long sizeOffset = getSymbol("gHotSpotVMTypeEntrySizeOffset");
        arrayStride = getSymbol("gHotSpotVMTypeEntryArrayStride");

        for (; ; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            if (typeName == null) break;

            String superclassName = getStringRef(entry + superclassNameOffset);
            boolean isOop = getInt(entry + isOopTypeOffset) != 0;
            boolean isInt = getInt(entry + isIntegerTypeOffset) != 0;
            boolean isUnsigned = getInt(entry + isUnsignedOffset) != 0;
            int size = getInt(entry + sizeOffset);

            Set<Field> fields = structs.get(typeName);
            types.put(typeName, new Type(typeName, superclassName, size, isOop, isInt, isUnsigned, fields));
        }

        entry = getSymbol("gHotSpotVMIntConstants");
        long nameOffset = getSymbol("gHotSpotVMIntConstantEntryNameOffset");
        long valueOffset = getSymbol("gHotSpotVMIntConstantEntryValueOffset");
        arrayStride = getSymbol("gHotSpotVMIntConstantEntryArrayStride");

        for (; ; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;

            int value = getInt(entry + valueOffset);
            constants.put(name, value);
        }

        entry = getSymbol("gHotSpotVMLongConstants");
        nameOffset = getSymbol("gHotSpotVMLongConstantEntryNameOffset");
        valueOffset = getSymbol("gHotSpotVMLongConstantEntryValueOffset");
        arrayStride = getSymbol("gHotSpotVMLongConstantEntryArrayStride");

        for (; ; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;

            long value = getLong(entry + valueOffset);
            constants.put(name, value);
        }
    }

    private static Path findFirstFile(Path directory, String... files) {
        for (String file : files) {
            Path path = directory.resolve(file);
            if (Files.exists(path)) return path;
        }
        throw new RuntimeException("Failed to find one of the required paths!: " + Arrays.toString(files));
    }

    public static byte getByte(long addr) {
        return unsafe.getByte(addr);
    }

    public static void putByte(long addr, byte val) {
        unsafe.putByte(addr, val);
    }

    public static short getShort(long addr) {
        return unsafe.getShort(addr);
    }

    public static void putShort(long addr, short val) {
        unsafe.putShort(addr, val);
    }

    public static int getInt(long addr) {
        return unsafe.getInt(addr);
    }

    public static void putInt(long addr, int val) {
        unsafe.putInt(addr, val);
    }

    public static long getLong(long addr) {
        return unsafe.getLong(addr);
    }

    public static void putLong(long addr, long val) {
        unsafe.putLong(addr, val);
    }

    public static long getAddress(long addr) {
        return unsafe.getAddress(addr);
    }

    public static void putAddress(long addr, long val) {
        unsafe.putAddress(addr, val);
    }

    public static String getString(long addr) {
        if (addr == 0) {
            return null;
        }

        char[] chars = new char[40];
        int offset = 0;
        for (byte b; (b = getByte(addr + offset)) != 0; ) {
            if (offset >= chars.length) chars = Arrays.copyOf(chars, offset * 2);
            chars[offset++] = (char) b;
        }
        return new String(chars, 0, offset);
    }

    public static String getStringRef(long addr) {
        return getString(getAddress(addr));
    }

    public static long getSymbol(String name) {
        long address = JVM.findEntry(name);
        if (address == 0) {
            throw new NoSuchElementException("No such symbol: " + name);
        }
        return getLong(address);
    }

    public static Type type(String name) {
        Type type = types.get(name);
        if (type == null) {
            throw new NoSuchElementException("No such type: " + name);
        }
        return type;
    }

    public static Number constant(String name) {
        Number constant = constants.get(name);
        if (constant == null) {
            throw new NoSuchElementException("No such constant: " + name);
        }
        return constant;
    }

    public static int intConstant(String name) {
        return constant(name).intValue();
    }

    public static long longConstant(String name) {
        return constant(name).longValue();
    }
}
