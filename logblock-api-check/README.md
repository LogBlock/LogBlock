# LogBlock API Check

This build-only Maven plugin checks that LogBlock's shared Core, compiled against
Spigot, is compatible with the Paper API version selected in the root POM. It is
never included in the server plugin.

Using ASM, it inspects compiled bytecode for incompatible class, method and field
references, including signatures, inherited members and access rules. The check
runs during `process-classes` for Core and again during `verify` for the Core
classes inside the shaded plugin JAR. The latter also verifies required classes
and resources and rejects bundled server API or checker classes.

Run the complete build from the repository root with Java 25:

```text
mvn clean verify
```

Incompatible references fail the build, including when using `-DskipTests`.
Reports are written to each checked module's `target/paper-api-check.txt`.

The checker validates static bytecode references; it does not check references
encoded as reflection strings, runtime behaviour or full JVM data-flow validity.
