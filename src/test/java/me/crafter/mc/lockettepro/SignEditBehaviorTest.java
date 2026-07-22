package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for sign editing behavior when a player right-clicks an existing
 * LocketteProMax lock sign and edits it via Minecraft's sign UI.
 *
 * <p>In Minecraft 26.2+, right-clicking any existing sign fires a
 * {@link SignChangeEvent} when the player closes the editor. These tests
 * verify that the plugin correctly handles this case.
 */
public class SignEditBehaviorTest extends LocketteProTestBase {

  private Block chestBlock;
  private Block signBlock;
  private Player owner;
  private Player stranger;
  private BlockPlayerListener listener;

  @BeforeEach
  public void setUpSign() {
    owner = server.addPlayer("Alice");
    stranger = server.addPlayer("Charlie");

    // Place a chest at (0, 64, 0)
    chestBlock = server.getWorld("world").getBlockAt(0, 64, 0);
    chestBlock.setType(Material.CHEST);

    // Place a wall sign at (0, 64, -1) facing NORTH (attached to the chest)
    signBlock = server.getWorld("world").getBlockAt(0, 64, -1);
    signBlock.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) signBlock.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    signBlock.setBlockData(wallSignData);

    // Lock the chest: [Private] / Alice on line 0 and 1
    Utils.setSignLine(signBlock, 0, "[Private]", true);
    Utils.setSignLine(signBlock, 1, "Alice", true);

    listener = new BlockPlayerListener();

    // Sanity checks
    assertTrue(LocketteProAPI.isLockSign(signBlock));
    assertTrue(LocketteProAPI.isLocked(chestBlock));
  }

  /** Simulate a SignChangeEvent on {@code signBlock} and fire it through the listener. */
  private SignChangeEvent fireSignChangeEvent(Player player, String... lines) {
    String[] paddedLines = new String[4];
    for (int i = 0; i < 4; i++) {
      paddedLines[i] = (i < lines.length) ? lines[i] : "";
    }
    SignChangeEvent event = new SignChangeEvent(signBlock, player, paddedLines);
    listener.onManualLock(event);
    return event;
  }

  /** Closing the sign editor without changes should pass through silently — no [ERROR]. */
  @Test
  public void testNoChangeSilent() {
    SignChangeEvent event = fireSignChangeEvent(owner, "[Private]", "Alice", "", "");
    assertEquals("[Private]", event.getLine(0));
  }

  /** Owner changing line 0 to a non-lock string must produce [ERROR] on line 0. */
  @Test
  public void testOwnerInvalidLine0GetsError() {
    SignChangeEvent event = fireSignChangeEvent(owner, "NotPrivate", "Alice", "", "");
    assertEquals("[ERROR]", event.getLine(0));
    assertTrue(LocketteProAPI.isLocked(chestBlock));
  }

  /** isSignError() detects the [ERROR] marker written to line 0. */
  @Test
  public void testIsSignErrorDetected() {
    Utils.setSignLine(signBlock, 0, "[ERROR]", true);
    assertTrue(LocketteProAPI.isSignError(signBlock));
    assertFalse(LocketteProAPI.isLockSign(signBlock));
  }

  /** After [ERROR] state, the owner can restore [Private] — event line 0 stays [Private]. */
  @Test
  public void testOwnerRestoresPrivateAfterError() {
    Utils.setSignLine(signBlock, 0, "[ERROR]", true);
    assertTrue(LocketteProAPI.isSignError(signBlock));

    SignChangeEvent event = fireSignChangeEvent(owner, "[Private]", "Alice", "", "");
    assertEquals("[Private]", event.getLine(0));
  }

  /** After [ERROR] state, a typo like [Private. must still produce [ERROR]. */
  @Test
  public void testOwnerTypoAfterErrorGetsError() {
    Utils.setSignLine(signBlock, 0, "[ERROR]", true);
    SignChangeEvent event = fireSignChangeEvent(owner, "[Private.", "Alice", "", "");
    assertEquals("[ERROR]", event.getLine(0));
  }

  /** isLockSign() must return true even when the sign's dye color is RED. */
  @Test
  public void testIsLockSignIgnoresSignDyeColor() {
    Sign signState = (Sign) signBlock.getState();
    Utils.setSignColor(signState, DyeColor.RED, true);

    assertTrue(LocketteProAPI.isLockSign(signBlock));
    assertFalse(LocketteProAPI.isSignError(signBlock));
  }

  /** A non-owner editing a lock sign has their changes reverted and gets denied. */
  @Test
  public void testStrangerCannotEditExistingSign() {
    SignChangeEvent event = fireSignChangeEvent(stranger, "[Private]", "Charlie", "", "");

    // Lines restored to original
    assertEquals("[Private]", event.getLine(0));
    assertEquals("Alice", event.getLine(1));
    assertTrue(LocketteProAPI.isOwner(chestBlock, owner));
    assertFalse(LocketteProAPI.isOwner(chestBlock, stranger));
  }

  /** Owner adding a user on line 2 should pass through silently with no error. */
  @Test
  public void testOwnerAddsUserSilently() {
    SignChangeEvent event = fireSignChangeEvent(owner, "[Private]", "Alice", "Charlie", "");
    assertEquals("[Private]", event.getLine(0));
    assertEquals("Charlie", event.getLine(2));
  }
}
