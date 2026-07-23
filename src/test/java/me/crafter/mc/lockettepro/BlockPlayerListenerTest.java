package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlockPlayerListenerTest extends LocketteProTestBase {

  private BlockPlayerListener listener;
  private Block chestBlock;
  private Block signBlock;
  private Player owner;
  private Player stranger;

  @BeforeEach
  public void setUpPlayerListener() {
    listener = new BlockPlayerListener();
    owner = server.addPlayer("Alice");
    stranger = server.addPlayer("Bob");

    // Give Alice permission to lock/use/edit
    owner.addAttachment(plugin, LockettePro.getPermission("lock"), true);
    owner.addAttachment(plugin, LockettePro.getPermission("edit"), true);

    // Set up locked chest at (0, 64, 0) with sign at (0, 64, -1) facing NORTH
    chestBlock = server.getWorld("world").getBlockAt(0, 64, 0);
    chestBlock.setType(Material.CHEST);

    signBlock = server.getWorld("world").getBlockAt(0, 64, -1);
    signBlock.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) signBlock.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    signBlock.setBlockData(wallSignData);

    Utils.setSignLine(signBlock, 0, "[Private]", true);
    Utils.setSignLine(signBlock, 1, "Alice", true);
  }

  @Test
  public void testQuickLockChest() {
    // New unlocked chest at (5, 64, 0)
    Block newChest = server.getWorld("world").getBlockAt(5, 64, 0);
    newChest.setType(Material.CHEST);

    owner.getInventory().setItemInMainHand(new ItemStack(Material.OAK_SIGN));

    PlayerInteractEvent event =
        new PlayerInteractEvent(
            owner,
            Action.RIGHT_CLICK_BLOCK,
            owner.getInventory().getItemInMainHand(),
            newChest,
            BlockFace.NORTH,
            EquipmentSlot.HAND);

    listener.onPlayerQuickLockChest(event);

    assertTrue(event.isCancelled());
    Block placedSign = newChest.getRelative(BlockFace.NORTH);
    assertTrue(LocketteProAPI.isSign(placedSign));
    assertTrue(LocketteProAPI.isLocked(newChest));
  }

  @Test
  public void testPlayerSelectSign() {
    PlayerInteractEvent event =
        new PlayerInteractEvent(
            owner, Action.RIGHT_CLICK_BLOCK, null, signBlock, BlockFace.NORTH, EquipmentSlot.HAND);

    listener.playerSelectSign(event);

    org.junit.jupiter.api.Assertions.assertEquals(signBlock, Utils.getSelectedSign(owner));
  }

  @Test
  public void testAttemptBreakSignOwnerAllowedStrangerDenied() {
    // Owner break sign -> allowed (event not cancelled)
    BlockBreakEvent ownerEvent = new BlockBreakEvent(signBlock, owner);
    listener.onAttemptBreakSign(ownerEvent);
    assertFalse(ownerEvent.isCancelled());

    // Stranger break sign -> denied (event cancelled)
    BlockBreakEvent strangerEvent = new BlockBreakEvent(signBlock, stranger);
    listener.onAttemptBreakSign(strangerEvent);
    assertTrue(strangerEvent.isCancelled());
  }

  @Test
  public void testAttemptBreakSignAdminAllowed() {
    stranger.addAttachment(plugin, LockettePro.getPermission("admin.break"), true);

    BlockBreakEvent event = new BlockBreakEvent(signBlock, stranger);
    listener.onAttemptBreakSign(event);

    assertFalse(event.isCancelled());
  }

  @Test
  public void testAttemptBreakLockedBlockDenied() {
    BlockBreakEvent event = new BlockBreakEvent(chestBlock, stranger);
    listener.onAttemptBreakLockedBlocks(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testAttemptInteractLockedBlockUserAllowedStrangerDenied() {
    // Owner interact -> allowed
    PlayerInteractEvent ownerEvent =
        new PlayerInteractEvent(
            owner, Action.RIGHT_CLICK_BLOCK, null, chestBlock, BlockFace.UP, EquipmentSlot.HAND);
    listener.onAttemptInteractLockedBlocks(ownerEvent);
    assertFalse(ownerEvent.isCancelled());

    // Stranger interact -> denied
    PlayerInteractEvent strangerEvent =
        new PlayerInteractEvent(
            stranger,
            Action.RIGHT_CLICK_BLOCK,
            null,
            chestBlock,
            BlockFace.UP,
            EquipmentSlot.HAND);
    listener.onAttemptInteractLockedBlocks(strangerEvent);
    assertTrue(strangerEvent.isCancelled());
  }

  @Test
  public void testAttemptPlaceInterfereBlocks() {
    // Attempt placing another chest adjacent to locked chest at (1, 64, 0)
    Block interfereBlock = server.getWorld("world").getBlockAt(1, 64, 0);
    interfereBlock.setType(Material.CHEST);

    BlockPlaceEvent event =
        new BlockPlaceEvent(
            interfereBlock,
            interfereBlock.getState(),
            chestBlock,
            new ItemStack(Material.CHEST),
            stranger,
            true,
            EquipmentSlot.HAND);

    listener.onAttemptPlaceInterfereBlocks(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testBucketEmptyAndFillProtection() {
    // Stranger bucket empty targeting chestBlock with BlockFace.NORTH -> relative is signBlock (protected)
    PlayerBucketEmptyEvent emptyEvent =
        new PlayerBucketEmptyEvent(
            stranger,
            chestBlock,
            chestBlock,
            BlockFace.NORTH,
            Material.WATER_BUCKET,
            new ItemStack(Material.WATER_BUCKET),
            EquipmentSlot.HAND);
    listener.onBucketEmpty(emptyEvent);
    assertTrue(emptyEvent.isCancelled());

    // Stranger bucket fill targeting chestBlock with BlockFace.NORTH -> relative is signBlock (protected)
    PlayerBucketFillEvent fillEvent =
        new PlayerBucketFillEvent(
            stranger,
            chestBlock,
            chestBlock,
            BlockFace.NORTH,
            Material.BUCKET,
            new ItemStack(Material.BUCKET),
            EquipmentSlot.HAND);
    listener.onBucketUse(fillEvent);
    assertTrue(fillEvent.isCancelled());
  }

  @Test
  public void testLecternTakeProtection() {
    Block lecternBlock = server.getWorld("world").getBlockAt(10, 64, 10);
    lecternBlock.setType(Material.LECTERN);

    Block lecternSign = server.getWorld("world").getBlockAt(10, 64, 9);
    lecternSign.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) lecternSign.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    lecternSign.setBlockData(wallSignData);

    Utils.setSignLine(lecternSign, 0, "[Private]", true);
    Utils.setSignLine(lecternSign, 1, "Alice", true);

    Lectern lecternState = (Lectern) lecternBlock.getState();

    PlayerTakeLecternBookEvent event = new PlayerTakeLecternBookEvent(stranger, lecternState);
    listener.onLecternTake(event);

    assertTrue(event.isCancelled());
  }
}
