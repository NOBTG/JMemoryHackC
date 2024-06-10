package memory.mem.jvm;

import memory.mem.CTool;
import memory.mem.array.Array;
import memory.mem.base.CObject;
import memory.mem.constants.ConstantPool;
import memory.mem.klass.InstanceKlass;
import memory.mem.method.ConstMethod;
import memory.mem.method.Method;
import memory.mem.base.Type;
import org.objectweb.asm.Opcodes;

import java.util.function.Function;

public final class CodeRunner {
    public static void rev(Class<?> target) {
        Type constMethodType = JVM.type("ConstMethod");
        Type constantPoolType = JVM.type("ConstantPool");
        long methodArray = InstanceKlass.of(target).getField(InstanceKlass.CField._methods, JVM::getAddress);
        new Array(methodArray, "Method*").forEach(aLong -> {
            ConstMethod constMethod = new ConstMethod(new Method(aLong).getField(Method.CField._constMethod, JVM::getAddress));

            ConstantPool constantPool = new ConstantPool(constMethod.getField(ConstMethod.CField._constants, JVM::getAddress));
            int nameIndex = constMethod.getField(ConstMethod.CField._name_index, JVM::getShort) & 0xffff;
            int signatureIndex = constMethod.getField(ConstMethod.CField._signature_index, JVM::getShort) & 0xffff;

            String name = CTool.getSymbol(constantPool.getPtr() + constantPoolType.size() + (long) nameIndex * CTool.oopSize);
            String desc = CTool.getSymbol(constantPool.getPtr() + constantPoolType.size() + (long) signatureIndex * CTool.oopSize);

            if (name.startsWith("<") || name.contains("$") || name.contains("[")) {
                return;
            }

            CObject address = new CObject(constMethod.getPtr() + constMethodType.size());

            int type = org.objectweb.asm.Type.getReturnType(desc).getSort();
            switch (type) {
                case org.objectweb.asm.Type.VOID -> JVM.putByte(address.getPtr(), (byte) Opcodes.RETURN);
                case org.objectweb.asm.Type.BOOLEAN, org.objectweb.asm.Type.INT, org.objectweb.asm.Type.CHAR,
                     org.objectweb.asm.Type.BYTE, org.objectweb.asm.Type.SHORT -> {
                    JVM.putByte(address.getPtr(), (byte) Opcodes.ICONST_0);
                    JVM.putByte(address.getPtr() + 1, (byte) Opcodes.IRETURN);
                }
                case org.objectweb.asm.Type.FLOAT -> {
                    JVM.putByte(address.getPtr(), (byte) Opcodes.FCONST_0);
                    JVM.putByte(address.getPtr() + 1, (byte) Opcodes.FRETURN);
                }
                case org.objectweb.asm.Type.LONG -> {
                    JVM.putByte(address.getPtr(), (byte) Opcodes.LCONST_0);
                    JVM.putByte(address.getPtr() + 1, (byte) Opcodes.LRETURN);
                }
                case org.objectweb.asm.Type.DOUBLE -> {
                    JVM.putByte(address.getPtr(), (byte) Opcodes.DCONST_0);
                    JVM.putByte(address.getPtr() + 1, (byte) Opcodes.DRETURN);
                }
                case org.objectweb.asm.Type.ARRAY, org.objectweb.asm.Type.OBJECT -> {
                    JVM.putByte(address.getPtr(), (byte) Opcodes.ACONST_NULL);
                    JVM.putByte(address.getPtr() + 1, (byte) Opcodes.ARETURN);
                }
            }
        });
    }
}

