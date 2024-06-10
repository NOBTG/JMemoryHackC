package memory.mem;

import memory.mem.array.Array;
import memory.mem.base.CObject;
import memory.mem.jvm.JVM;
import memory.mem.klass.InstanceKlass;
import memory.mem.method.ConstMethod;
import memory.mem.method.Method;
import org.objectweb.asm.Type;

import java.util.Arrays;

public final class ClassApi {
    public static void setMethodAccessFlag(Class<?> target, String methodName, int methodNewAccess, Class<?>... methodArgs) {
        setMethodAccessFlag(target, methodName, Type.getMethodDescriptor(Type.getType(methodArgs[0]), Arrays.stream(methodArgs, 1, methodArgs.length).map(Type::getType).toArray(Type[]::new)), methodNewAccess);
    }

    public static void setMethodAccessFlag(Class<?> target, String methodName, String methodDesc, int methodNewAccess) {
        long methods = InstanceKlass.of(target).getField(InstanceKlass.CField._methods, JVM::getAddress);
        new Array(methods, "Method*").forEach(l -> {
            Method method = new Method(l);
            ConstMethod constMethod = new ConstMethod(method.getField(Method.CField._constMethod, JVM::getAddress));

            CObject constantPool = new CObject(constMethod.getField(ConstMethod.CField._constants, JVM::getAddress));
            int nameIndex = constMethod.getField(ConstMethod.CField._name_index, JVM::getShort) & 0xffff;
            int signatureIndex = constMethod.getField(ConstMethod.CField._signature_index, JVM::getShort) & 0xffff;

            String name = CTool.getSymbol(constantPool.getPtr() + JVM.CONSTANT_POOL.size() + (long) nameIndex * CTool.oopSize);
            String desc = CTool.getSymbol(constantPool.getPtr() + JVM.CONSTANT_POOL.size() + (long) signatureIndex * CTool.oopSize);

            if (name.equals(methodName) && desc.equals(methodDesc)) {
                method.putField(Method.CField._access_flags, (short) methodNewAccess, JVM::putShort);
            }
        });
    }

    public static void redefineObjectClass(Object object, Class<?> clazz) {
        int address;
        try {
            address = JVM.unsafe.getIntVolatile(JVM.unsafe.allocateInstance(clazz), CTool.oopSize);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        JVM.unsafe.putIntVolatile(object, CTool.oopSize, address);
    }
}
