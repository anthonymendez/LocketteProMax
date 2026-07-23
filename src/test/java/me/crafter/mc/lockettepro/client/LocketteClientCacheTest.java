package me.crafter.mc.lockettepro.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import me.crafter.mc.lockettepro.LocketteProNetwork.BlockLockInfo;
import me.crafter.mc.lockettepro.LocketteProNetwork.LockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocketteClientCacheTest {

  @BeforeEach
  public void setUp() {
    LocketteClientCache.clearCache();
    LocketteClientCache.setOutlinesEnabled(true);
  }

  @Test
  public void testToggleOutlines() {
    assertTrue(LocketteClientCache.isOutlinesEnabled());
    LocketteClientCache.toggleOutlines();
    assertFalse(LocketteClientCache.isOutlinesEnabled());
    LocketteClientCache.toggleOutlines();
    assertTrue(LocketteClientCache.isOutlinesEnabled());
  }

  @Test
  public void testUpdateAndGetCache() {
    List<BlockLockInfo> list = new ArrayList<>();
    list.add(new BlockLockInfo(10, 64, 10, LockStatus.LOCKED_DENIED, "OwnerA"));
    list.add(new BlockLockInfo(20, 64, 20, LockStatus.LOCKED_ALLOWED, "OwnerB"));

    LocketteClientCache.updateCache(list);

    assertEquals(LockStatus.LOCKED_DENIED, LocketteClientCache.getLockStatus(10, 64, 10));
    assertEquals("OwnerA", LocketteClientCache.getLockInfo(10, 64, 10).getOwner());

    assertEquals(LockStatus.LOCKED_ALLOWED, LocketteClientCache.getLockStatus(20, 64, 20));
    assertEquals("OwnerB", LocketteClientCache.getLockInfo(20, 64, 20).getOwner());

    assertEquals(LockStatus.UNLOCKED, LocketteClientCache.getLockStatus(30, 64, 30));

    // Update position 10,64,10 to UNLOCKED should remove it from cache
    list.clear();
    list.add(new BlockLockInfo(10, 64, 10, LockStatus.UNLOCKED, ""));
    LocketteClientCache.updateCache(list);

    assertEquals(LockStatus.UNLOCKED, LocketteClientCache.getLockStatus(10, 64, 10));
    assertNull(LocketteClientCache.getLockInfo(10, 64, 10));
  }
}
