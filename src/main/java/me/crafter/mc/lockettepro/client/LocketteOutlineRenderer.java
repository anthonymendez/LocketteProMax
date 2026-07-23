package me.crafter.mc.lockettepro.client;

import me.crafter.mc.lockettepro.LocketteProNetwork.LockStatus;

/**
 * Visual renderer helper for drawing colored bounding box outlines on locked/unlocked containers.
 */
public class LocketteOutlineRenderer {

  // Color constants ARGB / RGB
  public static final int COLOR_LOCKED_DENIED = 0xFFFF0000; // Red
  public static final int COLOR_LOCKED_ALLOWED = 0xFF00FF00; // Green

  /**
   * Resolves ARGB outline color for a given LockStatus.
   */
  public static int getColorForStatus(LockStatus status) {
    if (status == LockStatus.LOCKED_DENIED) {
      return COLOR_LOCKED_DENIED;
    } else if (status == LockStatus.LOCKED_ALLOWED) {
      return COLOR_LOCKED_ALLOWED;
    }
    return 0; // Transparent for UNLOCKED
  }

  public static boolean shouldRender(LockStatus status) {
    return LocketteClientCache.isOutlinesEnabled() && status != LockStatus.UNLOCKED;
  }
}
