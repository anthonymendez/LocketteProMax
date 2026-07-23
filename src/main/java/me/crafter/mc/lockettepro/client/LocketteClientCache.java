package me.crafter.mc.lockettepro.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.crafter.mc.lockettepro.LocketteProNetwork.BlockLockInfo;
import me.crafter.mc.lockettepro.LocketteProNetwork.LockStatus;

/**
 * Client-side container lock status cache.
 * Stores lock statuses synced from the server network channel or determined via client-side fallbacks.
 */
public class LocketteClientCache {

  public static class BlockPosKey {
    private final int x;
    private final int y;
    private final int z;

    public BlockPosKey(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public int getZ() {
      return z;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BlockPosKey that = (BlockPosKey) o;
      return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, y, z);
    }
  }

  private static final Map<BlockPosKey, BlockLockInfo> cache = Collections.synchronizedMap(new HashMap<>());
  private static boolean outlinesEnabled = true;

  public static void setOutlinesEnabled(boolean enabled) {
    outlinesEnabled = enabled;
  }

  public static boolean isOutlinesEnabled() {
    return outlinesEnabled;
  }

  public static void toggleOutlines() {
    outlinesEnabled = !outlinesEnabled;
  }

  public static void updateCache(List<BlockLockInfo> lockInfos) {
    if (lockInfos == null) return;
    for (BlockLockInfo info : lockInfos) {
      if (info == null) continue;
      BlockPosKey key = new BlockPosKey(info.getX(), info.getY(), info.getZ());
      if (info.getStatus() == LockStatus.UNLOCKED) {
        cache.remove(key);
      } else {
        cache.put(key, info);
      }
    }
  }

  public static BlockLockInfo getLockInfo(int x, int y, int z) {
    return cache.get(new BlockPosKey(x, y, z));
  }

  public static LockStatus getLockStatus(int x, int y, int z) {
    BlockLockInfo info = getLockInfo(x, y, z);
    return info != null ? info.getStatus() : LockStatus.UNLOCKED;
  }

  public static void clearCache() {
    cache.clear();
  }

  public static Map<BlockPosKey, BlockLockInfo> getAllCachedLocks() {
    synchronized (cache) {
      return new HashMap<>(cache);
    }
  }
}
