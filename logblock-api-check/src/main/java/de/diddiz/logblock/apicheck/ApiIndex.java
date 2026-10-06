package de.diddiz.logblock.apicheck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Lazy metadata index. Looking up a class never loads or initializes that class. */
public final class ApiIndex implements AutoCloseable {
    record Member(String owner, String name, String descriptor, int access) {
    }

    static final class ApiClass {
        String name;
        String superName;
        String nestHost;
        int access;
        List<String> interfaces = List.of();
        final Map<String, Member> methods = new HashMap<>();
        final Map<String, Member> fields = new HashMap<>();
    }

    private interface ClassBytes {
        byte[] read() throws IOException;
    }

    private final Map<String, ClassBytes> sources = new HashMap<>();
    private final Map<String, ApiClass> parsed = new HashMap<>();
    private final List<JarFile> jars = new ArrayList<>();

    public void addClasses(Map<String, byte[]> classes) {
        classes.forEach((name, bytes) -> sources.put(name, () -> bytes));
    }

    public void addJar(Path file, boolean serverApi) throws IOException {
        JarFile jar = new JarFile(file.toFile(), true, JarFile.OPEN_READ, Runtime.version());
        jars.add(jar);
        // versionedStream presents the effective Java-25 entries of multi-release JARs.
        for (var entry : jar.versionedStream().filter(e -> e.getName().endsWith(".class")).toList()) {
            String name = entry.getName().substring(0, entry.getName().length() - 6);
            if (name.equals("module-info")) {
                continue;
            }
            if (isServerClass(name) && !serverApi) {
                throw new IOException("Server class " + name + " supplied by non-Paper artifact " + file);
            }
            sources.putIfAbsent(name, () -> {
                try (InputStream in = jar.getInputStream(entry)) {
                    return in.readAllBytes();
                }
            });
        }
    }

    static boolean isServerClass(String name) {
        return name.startsWith("org/bukkit/") || name.startsWith("org/spigotmc/")
                || name.startsWith("io/papermc/paper/") || name.startsWith("com/destroystokyo/paper/");
    }

    ApiClass find(String name) throws IOException {
        if (parsed.containsKey(name)) {
            return parsed.get(name);
        }
        byte[] bytes = null;
        ClassBytes source = sources.get(name);
        if (source != null) {
            bytes = source.read();
        } else {
            try (InputStream in = ClassLoader.getPlatformClassLoader().getResourceAsStream(name + ".class")) {
                if (in != null) {
                    bytes = in.readAllBytes();
                }
            }
        }
        if (bytes == null) {
            parsed.put(name, null);
            return null;
        }
        ApiClass info = new ApiClass();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String ownName, String signature, String superName, String[] interfaces) {
                info.name = ownName;
                info.superName = superName;
                info.nestHost = ownName;
                info.access = access;
                info.interfaces = List.of(interfaces);
            }

            @Override
            public void visitNestHost(String nestHost) {
                info.nestHost = nestHost;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                info.methods.put(name + descriptor, new Member(info.name, name, descriptor, access));
                return null;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                info.fields.put(name + ":" + descriptor, new Member(info.name, name, descriptor, access));
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        parsed.put(name, info);
        return info;
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (JarFile jar : jars) {
            try {
                jar.close();
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) throw failure;
    }
}
