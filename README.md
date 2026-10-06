LogBlock
==========

This plugin logs block changes such as breaking, placing, modifying, or burning to a MySQL Database. It can be used as an anti-griefing tool to find out who made a particular edit, or even roll back changes by certain players.
Originally written by bootswithdefer, for hMod, ported to Bukkit by me, because of the inability to identfy griefers. BigBrother also did't work, so I was forced to do it myself. The honor belongs to bootswithdefer for the sourcecode, I only spent about 8 hours to transcribe. All functions except sign text logging shold work as in hMod. The use of permissions plugin is possible, but not necessary.

You can download development builds [from our Jenkins server](https://www.iani.de/jenkins/job/LogBlock/).

## Building

LogBlock targets Minecraft 26.3 on Spigot and Paper and requires **JDK 25** and Maven 3.9.
Run the complete reactor from this directory:

```text
mvn clean verify
```

The verified plugin is available as `target/LogBlock.jar` and
`logblock-plugin/target/LogBlock.jar`. Install the same JAR on either server.
The root copy is produced only after the final API and packaging checks pass.
For PowerShell with a separate local Maven repository, quote the property:

```powershell
mvn '-Dmaven.repo.local=F:\ProgramData\MavenRepository' clean verify
```

### Modules

| Module | Purpose |
| --- | --- |
| `logblock-core` | Shared Bukkit logic, existing public packages and plugin entry point |
| `logblock-spigot` | Direct Spigot API calls and Bungee components |
| `logblock-paper` | Direct Paper API calls and Adventure components |
| `logblock-plugin` | Resources, shading, verification and the final plugin artifact |
| `logblock-api-check` | Build-only ASM checker; never included in the plugin |

The main class initializes the selected adapter before loading configuration or
initializing shared services. The Paper API marker is inspected without initializing
it. Only the selected adapter is loaded; initialization errors disable LogBlock
without falling back to another platform. The database, configuration and stored
data formats are unchanged.

### API compatibility

The root POM pins the Spigot and Paper API builds separately. Core compiles against
Spigot. Its resulting class files are checked against an isolated Paper dependency
graph in `process-classes`; `verify` checks the Core classes again inside the shaded
plugin and verifies the plugin's contents. There is no second compilation of Core.

The checker reads class metadata without loading server classes. It checks declared
and used types, complete method and field descriptors, inherited members, access,
invocation kind, annotations, generics, method references and bootstrap arguments.
Reports are written to each checked module's `target/paper-api-check.txt`.
API failures also fail builds using `-DskipTests`; that option skips unit tests only.
Reflection strings, event semantics and complete JVM data-flow verification are
outside the check's scope.

When updating Minecraft, update `spigot.api.version` and `paper.api.version` in the
root POM to explicit builds and run `mvn clean verify`. Spigot snapshots are pinned
by timestamp, rather than a moving `SNAPSHOT`. Keep a new API call in Core only if
the compatibility check passes. Otherwise, add the minimal differing operation to
the shared adapter contract and implement it separately on each platform.

For CI, use Java 25 and `mvn --batch-mode clean verify` and publish
`target/LogBlock.jar`. No live server or database is required for the automated tests.

### Manual server acceptance

Use the same verified JAR on separate Spigot and Paper 26.3 test servers with a test
database. Keep production data out of the acceptance run. Verify the following on
both servers, including with existing log data copied into the test database:

- Startup selects the expected adapter and enables LogBlock successfully.
- Commands, permissions and lookup tools behave as before.
- Block placement, breaking, inventory changes and lookups are logged correctly.
- Dripstone growth, merged tips and breaking supporting blocks are logged and
  can be rolled back correctly.
- Bookshelf slots and shelf chains in all four side-chain states log item swaps,
  including powered shelves and connected hotbar swaps.
- Colored messages, child text, command clicks, text hovers and item hovers render.
- Rollback and redo restore blocks and containers; existing saved items, player
  profiles and block states can be read and restored.
- Optional WorldEdit and WorldGuard integrations work when enabled and do not
  prevent startup when absent and disabled.

This refactor does not add Folia support or guarantee older Minecraft versions.
