---
name: test_server_helper
description: Diagnoses and resolves Minecraft/Paper development server startup issues (port conflicts, stale session locks, NTFS execution mapping blocks).
---

# Test Server Helper

Use this skill when you encounter issues running the test server using `./dev.sh run` or `./dev.sh`.

## Diagnostics Script
You can run the built-in diagnostic script at `.agents/skills/test_server_helper/scripts/diagnose.sh` to check for:
- Port conflicts on 25565 or 25566.
- Stale `session.lock` files blocking server start.
- Incorrect NTFS folder structure for the `spark` profiler.

## Action Steps for Common Issues
1. **Port conflicts**:
   - If port 25565/25566 is in use, modify `server-port` in `run/server.properties`.
2. **Session lock error**:
   - If `run/world/session.lock: already locked` occurs, delete the lock file: `rm -f run/world/session.lock`.
3. **Spark shared object mapping error**:
   - If the profiler fails with `failed to map segment from shared object`, ensure `run/plugins/spark` is a symlink to `/tmp/spark`. The `./dev.sh` script automatically configures this.
