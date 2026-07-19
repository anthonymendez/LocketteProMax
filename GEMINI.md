# Gemini Assistant Repository Guide - LocketteProMax

Welcome to the `LocketteProMax` repository. This guide outlines the stack, build configurations, coding rules, and development server workarounds for AI coding assistants working in this workspace.

## Development Stack
- **Target Platform**: Paper API (Minecraft 26.2 / Java 26)
- **Build System**: Gradle 9.6.1 (with Gradle Wrapper)
- **Java Version**: OpenJDK 26 (OpenJDK 26.0.1)

## Build & Properties Injection
Plugin configuration properties are managed in [gradle.properties](file:///mnt/Roommate/Projects/LocketteProMax/gradle.properties):
- `pluginName`: Injected into `plugin.yml` as `${pluginName}`.
- `pluginVersion`: Injected into `plugin.yml` as `${pluginVersion}`.
- `pluginMainCommand`: Injected into `plugin.yml` as `${pluginMainCommand}`.

Always use `./dev.sh build` or `./gradlew build` to ensure Gradle's resource filtering expands these tokens during compilation.

## Coding Guidelines

### 1. Dynamic Permissions
Never hardcode permission nodes (like `lockettepro.lock` or `lockettepromax.lock`) in Java. Use the static helper in `LockettePro` to construct nodes dynamically:
```java
String lockPermission = LockettePro.getPermission("lock"); // Resolves dynamically to e.g., "lockettepromax.lock"
```

### 2. Main Command Checks
Avoid hardcoding the command label check in command handlers. Check the command dynamically using the registered plugin name:
```java
if (cmd.getName().equalsIgnoreCase(plugin.getName())) { ... }
```

## Running & Testing Workarounds (NTFS mounts)
Because this repository resides on an NTFS filesystem (`/mnt/Roommate`), loading memory-mapped native files (such as Paper's bundled `spark` profiler's `libasyncProfiler.so`) directly from it fails with:
`failed to map segment from shared object`

**Always use the [dev.sh](file:///mnt/Roommate/Projects/LocketteProMax/dev.sh) wrapper to launch the server**:
- It automatically maps `run/plugins/spark` to the Linux memory partition `/tmp/spark` via a symbolic link.
- It sets `eula=true` in `run/eula.txt` and accepts the Minecraft EULA.
- It exports `TMPDIR=/tmp` at the shell level.
- It configures `jvmArgs("-Djava.io.tmpdir=/tmp")` for the test server JVM.
