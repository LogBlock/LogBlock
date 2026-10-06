package de.diddiz.logblock.apicheck;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.DependencyRequest;

/** Checks Core bytecode against the explicitly selected Paper API, also after shading. */
@Mojo(name = "check", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public final class CheckMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    @Component
    private RepositorySystem repositories;
    @Parameter(required = true)
    private String paperVersion;
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;
    @Parameter(defaultValue = "${project.build.directory}/paper-api-check.txt", required = true)
    private File reportFile;
    @Parameter
    private String coreArtifactId;
    @Parameter(defaultValue = "false")
    private boolean checkPackaging;

    private static final List<Exclusion> SERVER_EXCLUSIONS = List.of(
            new Exclusion("org.spigotmc", "spigot-api", "*", "*"),
            new Exclusion("org.bukkit", "bukkit", "*", "*"),
            new Exclusion("io.papermc.paper", "paper-api", "*", "*"),
            new Exclusion("com.destroystokyo.paper", "paper-api", "*", "*"));

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (Runtime.version().feature() != 25) {
            throw new MojoFailureException("The Paper API check requires the same Java 25 used to compile LogBlock");
        }
        try (ApiIndex index = new ApiIndex()) {
            MavenProject core = coreArtifactId == null ? project : session.getProjects().stream()
                    .filter(p -> p.getGroupId().equals(project.getGroupId()) && p.getArtifactId().equals(coreArtifactId))
                    .findFirst().orElseThrow(() -> new IOException("Core project is missing; run the build from the reactor root with -am"));
            Map<String, byte[]> subjects = readClasses(classesDirectory.toPath());
            Set<String> selectedNames = null;
            if (coreArtifactId != null) {
                File coreJar = core.getArtifact().getFile();
                if (coreJar == null || !coreJar.isFile()) throw new IOException("Core artifact has not been packaged");
                selectedNames = readClasses(coreJar.toPath()).keySet();
                Set<String> missing = new TreeSet<>(selectedNames);
                missing.removeAll(subjects.keySet());
                if (!missing.isEmpty()) throw new IOException("Core classes missing from plugin JAR: " + missing);
            }
            if (subjects.isEmpty()) throw new IOException("No class files to check in " + classesDirectory);
            if (subjects.keySet().stream().anyMatch(ApiIndex::isServerClass)) {
                throw new IOException("Core must not define classes in server API packages");
            }
            if (checkPackaging) verifyPackaging(subjects);
            if (selectedNames != null) subjects.keySet().retainAll(selectedNames);

            // The target root is Paper. No Spigot dependency is carried into this graph.
            var paper = new DefaultArtifact("io.papermc.paper:paper-api:" + paperVersion);
            CollectRequest collect = new CollectRequest();
            collect.setRoot(new Dependency(paper, "compile", false, SERVER_EXCLUSIONS));
            collect.setRepositories(project.getRemoteProjectRepositories());
            for (var dependency : core.getDependencies()) {
                if (dependency.getScope() != null && (dependency.getScope().equals("test") || dependency.getScope().equals("system"))) continue;
                if (SERVER_EXCLUSIONS.stream().anyMatch(e -> e.getGroupId().equals(dependency.getGroupId()) && e.getArtifactId().equals(dependency.getArtifactId()))) continue;
                collect.addDependency(new Dependency(new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                        dependency.getClassifier(), dependency.getType() == null ? "jar" : dependency.getType(), dependency.getVersion()),
                        "compile", false, SERVER_EXCLUSIONS));
            }
            var resolved = repositories.resolveDependencies(session.getRepositorySession(), new DependencyRequest(collect,
                    (node, parents) -> !node.getDependency().getScope().equals("test")));
            // Paper must be authoritative, even if a support artifact contains duplicate classes.
            var paperResult = resolved.getArtifactResults().stream()
                    .filter(a -> a.getArtifact().getGroupId().equals("io.papermc.paper") && a.getArtifact().getArtifactId().equals("paper-api"))
                    .findFirst().orElseThrow(() -> new IOException("Paper API was not resolved"));
            index.addJar(paperResult.getArtifact().getFile().toPath(), true);
            for (var artifact : resolved.getArtifactResults()) {
                if (artifact == paperResult || !artifact.getArtifact().getExtension().equals("jar")) continue;
                index.addJar(artifact.getArtifact().getFile().toPath(), false);
            }
            if (coreArtifactId != null) {
                // Shading changes Hikari's namespace. Use the actual bundled support classes.
                Map<String, byte[]> bundled = readClasses(classesDirectory.toPath());
                bundled.keySet().removeAll(subjects.keySet());
                index.addClasses(bundled);
            }
            var errors = new ApiChecker(index).check(subjects);
            List<String> report = new ArrayList<>();
            report.add("Paper API: " + paperVersion);
            report.add("Checked Core classes: " + subjects.size());
            report.add("Incompatible references: " + errors.size());
            report.addAll(errors);
            Files.createDirectories(reportFile.toPath().getParent());
            Files.write(reportFile.toPath(), report, StandardCharsets.UTF_8);
            errors.forEach(getLog()::error);
            if (!errors.isEmpty()) throw new MojoFailureException("Paper API check failed: " + errors.size() + " incompatible reference(s). Report: " + reportFile);
            getLog().info("Paper API check passed for " + subjects.size() + " Core classes against " + paperVersion);
        } catch (MojoFailureException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new MojoExecutionException("Could not perform the Paper API check", failure);
        }
    }

    static Map<String, byte[]> readClasses(Path path) throws IOException {
        Map<String, byte[]> classes = new HashMap<>();
        if (Files.isDirectory(path)) {
            try (var files = Files.walk(path)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                    String name = path.relativize(file).toString().replace(File.separatorChar, '/');
                    classes.put(name.substring(0, name.length() - 6), Files.readAllBytes(file));
                }
            }
        } else {
            try (JarFile jar = new JarFile(path.toFile())) {
                for (var entry : jar.stream().filter(e -> e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")).toList()) {
                    try (var in = jar.getInputStream(entry)) {
                        classes.put(entry.getName().substring(0, entry.getName().length() - 6), in.readAllBytes());
                    }
                }
            }
        }
        classes.remove("module-info");
        return classes;
    }

    private void verifyPackaging(Map<String, byte[]> classes) throws IOException {
        for (String name : classes.keySet()) {
            if (ApiIndex.isServerClass(name) || name.startsWith("org/objectweb/asm/") || name.startsWith("de/diddiz/logblock/apicheck/")) {
                throw new IOException("Forbidden class bundled in plugin: " + name);
            }
        }
        for (String name : List.of("de/diddiz/LogBlock/LogBlock", "de/diddiz/LogBlock/platform/spigot/SpigotPlatformAdapter",
                "de/diddiz/LogBlock/platform/paper/PaperPlatformAdapter", "de/diddiz/lib/com/zaxxer/hikari/HikariDataSource")) {
            if (!classes.containsKey(name)) throw new IOException("Required class missing from plugin: " + name);
        }
        try (JarFile jar = new JarFile(classesDirectory)) {
            for (String name : List.of("plugin.yml", "itemdata.txt", "blockdata.txt")) {
                if (jar.getJarEntry(name) == null) throw new IOException("Required resource missing from plugin: " + name);
            }
            try (var in = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {
                String descriptor = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (descriptor.contains("${") || !descriptor.contains("main: de.diddiz.LogBlock.LogBlock") || !descriptor.contains("name: LogBlock")) {
                    throw new IOException("Plugin descriptor has incorrect or unresolved metadata");
                }
            }
        }
    }
}
