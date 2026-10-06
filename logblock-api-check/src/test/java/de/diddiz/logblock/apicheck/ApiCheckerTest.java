package de.diddiz.logblock.apicheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedSet;
import javax.tools.ToolProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Compile real Java fixtures against one API, then check the exact bytes against another. */
public class ApiCheckerTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private Path compile(String className, String source, Path classpath) throws IOException {
        Path work = temporary.newFolder().toPath();
        Path file = work.resolve(className.replace('.', '/') + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source, StandardCharsets.UTF_8);
        Path output = work.resolve("classes");
        Files.createDirectories(output);
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "25", "-g",
                "-classpath", classpath == null ? output.toString() : classpath.toString(), "-d", output.toString(), file.toString());
        assertEquals("Fixture compilation must succeed", 0, status);
        return output;
    }

    private SortedSet<String> check(String sourceApi, String targetApi, String consumer) throws IOException {
        Path source = compile("api.Api", "package api; " + sourceApi, null);
        Path target = compile("api.Api", "package api; " + targetApi, null);
        Path core = compile("client.Core", "package client; import api.Api; " + consumer, source);
        try (ApiIndex index = new ApiIndex()) {
            index.addClasses(CheckMojo.readClasses(target));
            return new ApiChecker(index).check(CheckMojo.readClasses(core));
        }
    }

    private void failure(String source, String target, String consumer, String expected) throws IOException {
        var errors = check(source, target, consumer);
        assertTrue("Expected " + expected + ", got: " + errors, errors.stream().anyMatch(e -> e.contains(expected)));
    }

    @Test
    public void compatibleApiIncludingInheritedMembers() throws IOException {
        String api = "public class Api extends Base {} class Base { public String call(){return \"ok\";} public int count; }";
        assertTrue(check(api, api, "public class Core { public String test(Api a){return a.call() + a.count;} }").isEmpty());
    }

    @Test
    public void missingMethodAndExactDescriptor() throws IOException {
        failure("public class Api {public String call(){return null;}}", "public class Api {public Object call(){return null;}}",
                "public class Core {public String test(Api a){return a.call();}}", "Missing method api.Api.call()Ljava/lang/String;");
    }

    @Test
    public void missingFieldAndExactDescriptor() throws IOException {
        failure("public class Api {public int count;}", "public class Api {public long count;}",
                "public class Core {public int test(Api a){return a.count;}}", "Missing field api.Api.count:I");
    }

    private static final String WITH_TYPE = "public class Api { public static class Missing {} }";
    private static final String WITHOUT_TYPE = "public class Api {}";

    @Test
    public void missingSuperclass() throws IOException {
        failure(WITH_TYPE, WITHOUT_TYPE, "public class Core extends Api.Missing {}", "Missing class api.Api$Missing");
    }

    @Test
    public void missingInterface() throws IOException {
        failure("public class Api {public interface Missing {}}", WITHOUT_TYPE,
                "public class Core implements Api.Missing {}", "Missing class api.Api$Missing");
    }

    @Test
    public void missingParameterType() throws IOException {
        failure(WITH_TYPE, WITHOUT_TYPE, "public class Core {public void use(Api.Missing value){}}", "Missing class api.Api$Missing");
    }

    @Test
    public void missingReturnType() throws IOException {
        failure(WITH_TYPE, WITHOUT_TYPE, "public class Core {public Api.Missing result(){return null;}}", "Missing class api.Api$Missing");
    }

    @Test
    public void missingFieldType() throws IOException {
        failure(WITH_TYPE, WITHOUT_TYPE, "public class Core {public Api.Missing value;}", "Missing class api.Api$Missing");
    }

    @Test
    public void missingGenericType() throws IOException {
        failure(WITH_TYPE, WITHOUT_TYPE, "public class Core {public java.util.List<Api.Missing> values;}", "Missing class api.Api$Missing");
    }

    @Test
    public void missingClassLiteral() throws IOException {
        failure(WITH_TYPE, WITHOUT_TYPE, "public class Core {public Class<?> value(){return Api.Missing.class;}}", "Missing class api.Api$Missing");
    }

    @Test
    public void missingAnnotationType() throws IOException {
        failure("public class Api {public @interface Missing {}}", WITHOUT_TYPE,
                "@Api.Missing public class Core {}", "Missing class api.Api$Missing");
    }

    @Test
    public void incompatibleMethodReference() throws IOException {
        failure("public class Api {public String text(){return null;}}", WITHOUT_TYPE,
                "public class Core {public java.util.function.Function<Api,String> value(){return Api::text;}}", "Missing method api.Api.text");
    }

    @Test
    public void inaccessibleMethod() throws IOException {
        failure("public class Api {public void call(){}}", "public class Api {private void call(){}}",
                "public class Core {public void test(Api a){a.call();}}", "Inaccessible method");
    }

    @Test
    public void inaccessibleField() throws IOException {
        failure("public class Api {public int count;}", "public class Api {private int count;}",
                "public class Core {public int test(Api a){return a.count;}}", "Inaccessible field");
    }

    @Test
    public void staticMethodChangedToInstance() throws IOException {
        failure("public class Api {public static void call(){}}", "public class Api {public void call(){}}",
                "public class Core {public void test(){Api.call();}}", "Static/instance method mismatch");
    }

    @Test
    public void staticFieldChangedToInstance() throws IOException {
        failure("public class Api {public static int count;}", "public class Api {public int count;}",
                "public class Core {public int test(){return Api.count;}}", "Static/instance field mismatch");
    }

    @Test
    public void fieldChangedToFinal() throws IOException {
        failure("public class Api {public int count;}", "public class Api {public final int count=0;}",
                "public class Core {public void test(Api a){a.count=1;}}", "Cannot write final field");
    }

    @Test
    public void superclassChangedToFinal() throws IOException {
        failure("public class Api {}", "public final class Api {}", "public class Core extends Api {}", "Invalid superclass");
    }

    @Test
    public void classChangedToAbstract() throws IOException {
        failure("public class Api {}", "public abstract class Api {}", "public class Core {public Api test(){return new Api();}}",
                "Cannot instantiate abstract class/interface");
    }

    @Test
    public void reflectionStringsAreOutsideStaticCheck() throws IOException {
        assertTrue(check(WITH_TYPE, WITHOUT_TYPE,
                "public class Core {public Class<?> test() throws Exception {return Class.forName(\"api.Api$Missing\");}}").isEmpty());
    }

    @Test
    public void shelfSourceCompilesTwiceButOneBinaryIsIncompatible() throws IOException {
        String spigot = "public class Api {public enum SideChain {LEFT} public SideChain getSideChain(){return SideChain.LEFT;}}";
        String paper = "public class Api {public enum ChainPart {LEFT} public ChainPart getSideChain(){return ChainPart.LEFT;}}";
        String consumer = "public class Core {public String name(Api shelf){return shelf.getSideChain().name();}}";
        Path paperApi = compile("api.Api", "package api; " + paper, null);
        compile("client.Core", "package client; import api.Api; " + consumer, paperApi);
        failure(spigot, paper, consumer, "Missing method api.Api.getSideChain()Lapi/Api$SideChain;");
    }

    @Test
    public void serverClassesCannotBeSuppliedBySupportJar() throws IOException {
        Path classes = compile("org.bukkit.Accidental", "package org.bukkit; public class Accidental {}", null);
        Path jar = temporary.newFile("support.jar").toPath();
        try (var out = new java.util.jar.JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new java.util.jar.JarEntry("org/bukkit/Accidental.class"));
            out.write(Files.readAllBytes(classes.resolve("org/bukkit/Accidental.class")));
            out.closeEntry();
        }
        try (ApiIndex index = new ApiIndex()) {
            assertThrows(IOException.class, () -> index.addJar(jar, false));
        }
    }
}
