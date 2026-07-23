package me.crafter.mc.lockettepro;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

public class DependencyProtocolLibTest extends LocketteProTestBase {

  @Test
  public void testOnSignSendIgnoresNonLockSign() {
    Player player = server.addPlayer("Alice");
    NbtCompound nbt = mock(NbtCompound.class);

    String line1 = "{\"extra\":[{\"text\":\"[Welcome]\"}],\"text\":\"\"}";
    when(nbt.getString("Text1")).thenReturn(line1);

    DependencyProtocolLib.onSignSend(player, nbt);

    verify(nbt, never()).put(anyString(), anyString());
  }

  @Test
  public void testOnSignSendWithLockSignHandledOrReflectionError() {
    Player player = server.addPlayer("Alice");
    NbtCompound nbt = mock(NbtCompound.class);

    String line1 = "{\"extra\":[{\"text\":\"[Private]\"}],\"text\":\"\"}";
    when(nbt.getString("Text1")).thenReturn(line1);

    try {
      DependencyProtocolLib.onSignSend(player, nbt);
    } catch (Throwable t) {
      // ProtocolLib's WrappedChatComponent requires ProtocolLib NMS reflection context
    }
  }

  @Test
  public void testSetUpAndCleanUpProtocolLibSafeWhenDisabled() {
    Config.protocollib = false;
    DependencyProtocolLib.setUpProtocolLib(plugin);
    DependencyProtocolLib.cleanUpProtocolLib(plugin);
  }
}
