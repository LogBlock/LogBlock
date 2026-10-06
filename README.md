LogBlock
==========

This plugin logs block changes such as breaking, placing, modifying, or burning to a MySQL Database. It can be used as an anti-griefing tool to find out who made a particular edit, or even roll back changes by certain players.
Originally written by bootswithdefer, for hMod, ported to Bukkit by me, because of the inability to identfy griefers. BigBrother also did't work, so I was forced to do it myself. The honor belongs to bootswithdefer for the sourcecode, I only spent about 8 hours to transcribe. All functions except sign text logging shold work as in hMod. The use of permissions plugin is possible, but not necessary.

You can download development builds [from our Jenkins server](https://www.iani.de/jenkins/job/LogBlock/).

## Building

LogBlock targets Spigot and Paper.
Run the complete reactor from this directory:

```text
mvn clean verify
```

The plugin is available as `target/LogBlock.jar`.

### Modules

| Module | Purpose |
| --- | --- |
| `logblock-core` | Shared Bukkit logic, public packages and plugin entry point |
| `logblock-spigot` | Direct Spigot API calls and Bungee components |
| `logblock-paper` | Direct Paper API calls and Adventure components |
| `logblock-plugin` | Resources, shading, verification and the final plugin artifact |
| `logblock-api-check` | Build-only ASM checker; never included in the plugin |

### API compatibility

The root POM pins the Spigot and Paper API builds separately. Core compiles against
Spigot. Its resulting class files are checked against an isolated Paper dependency
graph in `process-classes`; `verify` checks the Core classes again inside the shaded
plugin and verifies the plugin's contents.

The checker checks declared
and used types, complete method and field descriptors, inherited members, access,
invocation kind, annotations, generics, method references and bootstrap arguments.
Reports are written to each checked module's `target/paper-api-check.txt`.
API failures also fail builds using `-DskipTests`; that option skips unit tests only.
Reflection strings, event semantics and complete JVM data-flow verification are
outside the check's scope.

### Updating

When updating Minecraft, update `spigot.api.version` and `paper.api.version` in the
root POM to explicit builds and run `mvn clean verify`. Spigot snapshots are pinned
by timestamp, rather than a moving `SNAPSHOT`. Keep a new API call in Core only if
the compatibility check passes. Otherwise, add the minimal differing operation to
the shared adapter contract and implement it separately on each platform.

For CI, use Java 25 and `mvn --batch-mode clean verify` and publish
`target/LogBlock.jar`.
