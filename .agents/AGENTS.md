# Agent Instructions - LocketteProMax

These rules are loaded automatically by Antigravity when working in the `LocketteProMax` workspace.

## Stack & Build Configurations
- **Target Platform**: Paper API (Minecraft 26.2 / Java 26)
- **Java Version**: OpenJDK 26 (OpenJDK 26.0.1)
- **Gradle Version**: 9.6.1 (with Gradle Wrapper)
- **Properties**: [gradle.properties](file:///mnt/Roommate/Projects/LocketteProMax/gradle.properties) defines `pluginName`, `pluginVersion`, and `pluginMainCommand`.

## Code Guidelines
- **Permissions**: Never hardcode `lockettepro.*` or `lockettepromax.*` permission nodes in Java. Always construct them dynamically using `LockettePro.getPermission("node_name")`.
- **Commands**: Check the main command dynamically in command handlers using `cmd.getName().equalsIgnoreCase(plugin.getName())`.
- **Resource Processing**: Ensure Gradle resource templates (`plugin.yml`) are not broken by hardcoding names/versions.

## Dev Server & NTFS Workarounds
- The repository resides on an NTFS partition. Executing native libraries (like the `spark` profiler's `.so` file) from NTFS causes `failed to map segment from shared object`.
- **Workaround**: We symlink `run/plugins/spark` to `/tmp/spark` and set `TMPDIR=/tmp`.
- Always run the server using `./dev.sh` or `./dev.sh run` to ensure this symlink is created and `TMPDIR=/tmp` is set.
- If the server crashes or fails to bind, run the diagnostics tool: `.agents/skills/test_server_helper/scripts/diagnose.sh`.
