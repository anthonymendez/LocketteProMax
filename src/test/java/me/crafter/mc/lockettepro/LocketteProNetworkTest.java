package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import me.crafter.mc.lockettepro.LocketteProNetwork.BlockLockInfo;
import me.crafter.mc.lockettepro.LocketteProNetwork.LockStatus;
import org.junit.jupiter.api.Test;

public class LocketteProNetworkTest {

  @Test
  public void testEncodeDecodePayload() throws IOException {
    List<BlockLockInfo> originalList = new ArrayList<>();
    originalList.add(new BlockLockInfo(100, 64, -200, LockStatus.LOCKED_DENIED, "Player1"));
    originalList.add(new BlockLockInfo(101, 64, -200, LockStatus.LOCKED_ALLOWED, "Player2"));
    originalList.add(new BlockLockInfo(102, 64, -200, LockStatus.UNLOCKED, ""));

    byte[] payload = LocketteProNetwork.encodePayload(originalList);
    assertNotNull(payload);
    assertTrue(payload.length > 0);

    List<BlockLockInfo> decodedList = LocketteProNetwork.decodePayload(payload);
    assertEquals(originalList.size(), decodedList.size());

    for (int i = 0; i < originalList.size(); i++) {
      assertEquals(originalList.get(i), decodedList.get(i));
    }
  }

  @Test
  public void testLockStatusFromCode() {
    assertEquals(LockStatus.UNLOCKED, LockStatus.fromCode((byte) 0));
    assertEquals(LockStatus.LOCKED_ALLOWED, LockStatus.fromCode((byte) 1));
    assertEquals(LockStatus.LOCKED_DENIED, LockStatus.fromCode((byte) 2));
    assertEquals(LockStatus.UNLOCKED, LockStatus.fromCode((byte) 99));
  }
}
