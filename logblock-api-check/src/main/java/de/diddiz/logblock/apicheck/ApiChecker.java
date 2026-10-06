package de.diddiz.logblock.apicheck;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/** Checks symbolic references, not event semantics or JVM data-flow verification. */
public final class ApiChecker {
    private final ApiIndex index;
    private final SortedSet<String> errors = new TreeSet<>();

    public ApiChecker(ApiIndex index) {
        this.index = index;
    }

    public SortedSet<String> check(Map<String, byte[]> subjects) throws IOException {
        errors.clear();
        index.addClasses(subjects);
        try {
            subjects.values().forEach(bytes -> new ClassReader(bytes).accept(new References(), 0));
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        }
        return new TreeSet<>(errors);
    }

    private ApiIndex.ApiClass find(String name) {
        try {
            return index.find(name);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private boolean subtype(String child, String parent, Set<String> visited) {
        if (child == null || !visited.add(child)) return false;
        if (child.equals(parent)) return true;
        var info = find(child);
        return info != null && (subtype(info.superName, parent, visited)
                || info.interfaces.stream().anyMatch(i -> subtype(i, parent, visited)));
    }

    private static String packageName(String name) {
        return name.substring(0, Math.max(0, name.lastIndexOf('/')));
    }

    private boolean accessible(int access, String owner, String caller) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) return true;
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            var declaring = find(owner);
            var calling = find(caller);
            return declaring != null && calling != null && declaring.nestHost.equals(calling.nestHost);
        }
        return packageName(owner).equals(packageName(caller))
                || ((access & Opcodes.ACC_PROTECTED) != 0 && subtype(caller, owner, new HashSet<>()));
    }

    private ApiIndex.Member method(String owner, String key, Set<String> visited) {
        if (owner == null || !visited.add(owner)) return null;
        var info = find(owner);
        if (info == null) return null;
        var own = info.methods.get(key);
        if (own != null) return own;
        // Constructors are never inherited.
        if (key.startsWith("<")) return null;
        var inherited = method(info.superName, key, visited);
        if (inherited != null) return inherited;
        // Public instance Object methods may be referenced through an interface.
        if ((info.access & Opcodes.ACC_INTERFACE) != 0) {
            var object = find("java/lang/Object").methods.get(key);
            if (object != null && (object.access() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC) return object;
        }
        for (String iface : info.interfaces) {
            var member = method(iface, key, visited);
            if (member != null && (member.access() & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0) return member;
        }
        return null;
    }

    private ApiIndex.Member field(String owner, String key, Set<String> visited) {
        if (owner == null || !visited.add(owner)) return null;
        var info = find(owner);
        if (info == null) return null;
        var own = info.fields.get(key);
        if (own != null) return own;
        for (String iface : info.interfaces) {
            var member = field(iface, key, visited);
            if (member != null) return member;
        }
        return field(info.superName, key, visited);
    }

    private final class References extends ClassVisitor {
        private String caller;
        private String source;
        private String member = "<class>";
        private int line;

        References() { super(Opcodes.ASM9); }

        private void error(String message) {
            errors.add(caller.replace('/', '.') + "#" + member
                    + (source == null ? "" : " (" + source + (line > 0 ? ":" + line : "") + ")") + ": " + message);
        }

        private void classReference(String name) {
            if (name == null) return;
            if (name.startsWith("[")) { type(Type.getType(name)); return; }
            var target = find(name);
            if (target == null) error("Missing class " + name.replace('/', '.'));
            else if (!accessible(target.access, name, caller)) error("Inaccessible class " + name.replace('/', '.'));
        }

        private void type(Type type) {
            switch (type.getSort()) {
                case Type.ARRAY -> type(type.getElementType());
                case Type.OBJECT -> classReference(type.getInternalName());
                case Type.METHOD -> {
                    for (Type argument : type.getArgumentTypes()) type(argument);
                    type(type.getReturnType());
                }
                default -> { }
            }
        }

        private void descriptor(String desc) { if (desc != null) type(Type.getType(desc)); }

        private void signature(String signature, boolean typeOnly) {
            if (signature == null) return;
            if (typeOnly) new SignatureReader(signature).acceptType(signatureVisitor());
            else new SignatureReader(signature).accept(signatureVisitor());
        }

        private SignatureVisitor signatureVisitor() {
            return new SignatureVisitor(Opcodes.ASM9) {
                private String name;
                @Override public void visitClassType(String value) { name = value; classReference(name); }
                @Override public void visitInnerClassType(String value) { name += "$" + value; classReference(name); }
                @Override public SignatureVisitor visitArrayType() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitClassBound() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitInterfaceBound() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitSuperclass() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitInterface() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitParameterType() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitReturnType() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitExceptionType() { return newSignatureVisitor(); }
                @Override public SignatureVisitor visitTypeArgument(char wildcard) { return newSignatureVisitor(); }
                private SignatureVisitor newSignatureVisitor() {
                    // Each nested type needs its own outer-class name when visiting inner classes.
                    return signatureVisitor();
                }
            };
        }

        private AnnotationVisitor annotation(String desc) {
            descriptor(desc);
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override public void visit(String name, Object value) { constant(value); }
                @Override public void visitEnum(String name, String descriptor, String value) {
                    References.this.descriptor(descriptor);
                    fieldReference(Opcodes.GETSTATIC, Type.getType(descriptor).getInternalName(), value, descriptor);
                }
                @Override public AnnotationVisitor visitAnnotation(String name, String descriptor) { return annotation(descriptor); }
                @Override public AnnotationVisitor visitArray(String name) { return this; }
            };
        }

        private void constant(Object value) {
            if (value instanceof Type t) type(t);
            else if (value instanceof Handle handle) handle(handle);
            else if (value instanceof ConstantDynamic dynamic) {
                descriptor(dynamic.getDescriptor());
                handle(dynamic.getBootstrapMethod());
                for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) constant(dynamic.getBootstrapMethodArgument(i));
            }
        }

        private void handle(Handle handle) {
            int opcode = switch (handle.getTag()) {
                case Opcodes.H_GETFIELD -> Opcodes.GETFIELD;
                case Opcodes.H_GETSTATIC -> Opcodes.GETSTATIC;
                case Opcodes.H_PUTFIELD -> Opcodes.PUTFIELD;
                case Opcodes.H_PUTSTATIC -> Opcodes.PUTSTATIC;
                case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
                case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
                case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> Opcodes.INVOKESPECIAL;
                default -> Opcodes.INVOKEVIRTUAL;
            };
            if (handle.getTag() <= Opcodes.H_PUTSTATIC) fieldReference(opcode, handle.getOwner(), handle.getName(), handle.getDesc());
            else methodReference(opcode, handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
        }

        private void methodReference(int opcode, String owner, String name, String desc, boolean iface) {
            classReference(owner);
            descriptor(desc);
            if (owner.startsWith("[")) return; // Array clone is supplied by the JVM.
            var target = find(owner);
            if (target == null) return;
            if (((target.access & Opcodes.ACC_INTERFACE) != 0) != iface) error("Class/interface invocation mismatch: " + owner + "." + name + desc);
            var resolved = method(owner, name + desc, new HashSet<>());
            // MethodHandle and VarHandle have signature-polymorphic native methods.
            if (resolved == null && (owner.equals("java/lang/invoke/MethodHandle") || owner.equals("java/lang/invoke/VarHandle"))) {
                resolved = target.methods.values().stream().filter(m -> m.name().equals(name)
                        && (m.access() & (Opcodes.ACC_NATIVE | Opcodes.ACC_VARARGS)) == (Opcodes.ACC_NATIVE | Opcodes.ACC_VARARGS)).findFirst().orElse(null);
            }
            if (resolved == null) error("Missing method " + owner.replace('/', '.') + "." + name + desc);
            else {
                if (!accessible(resolved.access(), resolved.owner(), caller)) error("Inaccessible method " + owner + "." + name + desc);
                if (((resolved.access() & Opcodes.ACC_STATIC) != 0) != (opcode == Opcodes.INVOKESTATIC)) error("Static/instance method mismatch: " + owner + "." + name + desc);
                if ((target.access & Opcodes.ACC_INTERFACE) != 0 && opcode == Opcodes.INVOKESTATIC && !resolved.owner().equals(owner)) error("Interface static method is not inherited: " + owner + "." + name + desc);
            }
        }

        private void fieldReference(int opcode, String owner, String name, String desc) {
            classReference(owner);
            descriptor(desc);
            var resolved = field(owner, name + ":" + desc, new HashSet<>());
            if (resolved == null) error("Missing field " + owner.replace('/', '.') + "." + name + ":" + desc);
            else {
                if (!accessible(resolved.access(), resolved.owner(), caller)) error("Inaccessible field " + owner + "." + name + ":" + desc);
                boolean staticInstruction = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
                if (((resolved.access() & Opcodes.ACC_STATIC) != 0) != staticInstruction) error("Static/instance field mismatch: " + owner + "." + name);
                if ((resolved.access() & Opcodes.ACC_FINAL) != 0 && (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC)
                        && (!caller.equals(resolved.owner()) || !(staticInstruction ? member.startsWith("<clinit>") : member.startsWith("<init>")))) {
                    error("Cannot write final field " + owner + "." + name);
                }
            }
        }

        @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            caller = name;
            classReference(superName);
            var parent = superName == null ? null : find(superName);
            if (parent != null && (parent.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_FINAL)) != 0) {
                error("Invalid superclass " + superName);
            }
            for (String iface : interfaces) {
                classReference(iface);
                var implemented = find(iface);
                if (implemented != null && (implemented.access & Opcodes.ACC_INTERFACE) == 0) error("Expected interface " + iface);
            }
            signature(signature, false);
        }
        @Override public void visitSource(String source, String debug) { this.source = source; }
        @Override public void visitOuterClass(String owner, String name, String descriptor) {
            classReference(owner);
            if (name != null) {
                descriptor(descriptor);
                if (method(owner, name + descriptor, new HashSet<>()) == null) error("Missing enclosing method " + owner + "." + name + descriptor);
            }
        }
        @Override public void visitNestHost(String name) { classReference(name); }
        @Override public void visitNestMember(String name) { classReference(name); }
        @Override public void visitPermittedSubclass(String name) { classReference(name); }
        @Override public void visitInnerClass(String name, String outerName, String innerName, int access) { classReference(name); classReference(outerName); }
        @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) { return annotation(descriptor); }
        @Override public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) { return annotation(descriptor); }
        @Override public RecordComponentVisitor visitRecordComponent(String name, String desc, String sig) {
            descriptor(desc); signature(sig, true);
            return new RecordComponentVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return annotation(desc); }
                @Override public AnnotationVisitor visitTypeAnnotation(int ref, TypePath path, String desc, boolean visible) { return annotation(desc); }
            };
        }
        @Override public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
            member = name; line = 0;
            descriptor(desc); signature(sig, true); constant(value);
            return new FieldVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return annotation(desc); }
                @Override public AnnotationVisitor visitTypeAnnotation(int ref, TypePath path, String desc, boolean visible) { return annotation(desc); }
            };
        }
        @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
            member = name + desc; line = 0;
            descriptor(desc); signature(sig, false);
            if (exceptions != null) for (String exception : exceptions) classReference(exception);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override public void visitLineNumber(int number, Label label) { line = number; }
                @Override public void visitMethodInsn(int op, String owner, String name, String desc, boolean iface) { methodReference(op, owner, name, desc, iface); }
                @Override public void visitFieldInsn(int op, String owner, String name, String desc) { fieldReference(op, owner, name, desc); }
                @Override public void visitTypeInsn(int op, String name) {
                    classReference(name);
                    var target = name.startsWith("[") ? null : find(name);
                    if (op == Opcodes.NEW && target != null && (target.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) != 0) {
                        error("Cannot instantiate abstract class/interface " + name);
                    }
                }
                @Override public void visitMultiANewArrayInsn(String desc, int dimensions) { descriptor(desc); }
                @Override public void visitLdcInsn(Object value) { constant(value); }
                @Override public void visitInvokeDynamicInsn(String name, String desc, Handle bootstrap, Object... arguments) {
                    descriptor(desc); handle(bootstrap);
                    for (Object value : arguments) constant(value);
                }
                @Override public void visitTryCatchBlock(Label start, Label end, Label handler, String type) { classReference(type); }
                @Override public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int slot) { descriptor(desc); signature(sig, true); }
                @Override public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
                    if (local != null) for (Object value : local) if (value instanceof String name) classReference(name);
                    if (stack != null) for (Object value : stack) if (value instanceof String name) classReference(name);
                }
                @Override public AnnotationVisitor visitAnnotationDefault() { return annotation(null); }
                @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return annotation(desc); }
                @Override public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) { return annotation(desc); }
                @Override public AnnotationVisitor visitTypeAnnotation(int ref, TypePath path, String desc, boolean visible) { return annotation(desc); }
                @Override public AnnotationVisitor visitInsnAnnotation(int ref, TypePath path, String desc, boolean visible) { return annotation(desc); }
                @Override public AnnotationVisitor visitTryCatchAnnotation(int ref, TypePath path, String desc, boolean visible) { return annotation(desc); }
                @Override public AnnotationVisitor visitLocalVariableAnnotation(int ref, TypePath path, Label[] start, Label[] end, int[] index, String desc, boolean visible) { return annotation(desc); }
            };
        }
    }
}
