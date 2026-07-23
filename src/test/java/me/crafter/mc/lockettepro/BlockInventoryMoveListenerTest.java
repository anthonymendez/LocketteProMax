package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlockInventoryMoveListenerTest extends LocketteProTestBase {

  private BlockInventoryMoveListener listener;
  private Block chestBlock;
  private Block signBlock;
  private Block unprotectedChestBlock;
  private Player owner;

  @BeforeEach
  public void setUpMoveListener() {
    listener = new BlockInventoryMoveListener();
    owner = server.addPlayer("Alice");

    // Locked chest at (0, 64, 0)
    chestBlock = server.getWorld("world").getBlockAt(0, 64, 0);
    chestBlock.setType(Material.CHEST);

    signBlock = server.getWorld("world").getBlockAt(0, 64, -1);
    signBlock.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) signBlock.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    signBlock.setBlockData(wallSignData);

    Utils.setSignLine(signBlock, 0, "[Private]", true);
    Utils.setSignLine(signBlock, 1, "Alice", true);

    // Unprotected chest at (10, 64, 0)
    unprotectedChestBlock = server.getWorld("world").getBlockAt(10, 64, 0);
    unprotectedChestBlock.setType(Material.CHEST);
  }

  @Test
  public void testItemTransferOutBlockedForLockedChest() {
    plugin.getConfig().set("block-item-transfer-out", true);
    plugin.saveConfig();
    Config.reload();

    Chest lockedChestState = (Chest) chestBlock.getState();
    Chest unprotectedChestState = (Chest) unprotectedChestBlock.getState();

    InventoryMoveItemEvent event =
        new InventoryMoveItemEvent(
            lockedChestState.getInventory(),
            new ItemStack(Material.DIAMOND),
            unprotectedChestState.getInventory(),
            false);

    listener.onInventoryMove(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testItemTransferInBlockedForLockedChest() {
    plugin.getConfig().set("block-item-transfer-in", true);
    plugin.saveConfig();
    Config.reload();

    Chest lockedChestState = (Chest) chestBlock.getState();
    Chest unprotectedChestState = (Chest) unprotectedChestBlock.getState();

    InventoryMoveItemEvent event =
        new InventoryMoveItemEvent(
            unprotectedChestState.getInventory(),
            new ItemStack(Material.DIAMOND),
            lockedChestState.getInventory(),
            false);

    listener.onInventoryMove(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testHopperMinecartActionRemove() {
    plugin.getConfig().set("block-item-transfer-out", true);
    plugin.getConfig().set("block-hopper-minecart", "remove");
    plugin.saveConfig();
    Config.reload();

    Chest lockedChestState = (Chest) chestBlock.getState();
    HopperMinecart mockMinecart = mock(HopperMinecart.class);
    org.bukkit.inventory.Inventory minecartInv = mock(org.bukkit.inventory.Inventory.class);
    when(minecartInv.getHolder()).thenReturn(mockMinecart);

    InventoryMoveItemEvent event =
        new InventoryMoveItemEvent(
            lockedChestState.getInventory(),
            new ItemStack(Material.DIAMOND),
            minecartInv,
            false);

    listener.onInventoryMove(event);

    assertTrue(event.isCancelled());
    verify(mockMinecart).remove();
  }

  @Test
  public void testSameOwnerHopperTransferAllowed() {
    plugin.getConfig().set("block-item-transfer-out", true);
    plugin.saveConfig();
    Config.reload();

    // Create second chest locked by Alice at (0, 64, 5)
    Block destChestBlock = server.getWorld("world").getBlockAt(0, 64, 5);
    destChestBlock.setType(Material.CHEST);
    Block destSignBlock = server.getWorld("world").getBlockAt(0, 64, 4);
    destSignBlock.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) destSignBlock.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    destSignBlock.setBlockData(wallSignData);
    Utils.setSignLine(destSignBlock, 0, "[Private]", true);
    Utils.setSignLine(destSignBlock, 1, "Alice", true);

    Chest destState = (Chest) destChestBlock.getState();

    // Mock inventory holders to act as Hopper holders for same owner check
    org.bukkit.block.Hopper mockHopper = mock(org.bukkit.block.Hopper.class);
    when(mockHopper.getBlock()).thenReturn(chestBlock);

    org.bukkit.inventory.Inventory sourceInv = mock(org.bukkit.inventory.Inventory.class);
    when(sourceInv.getHolder()).thenReturn(mockHopper);
    when(sourceInv.getLocation()).thenReturn(chestBlock.getLocation());

    org.bukkit.inventory.Inventory destInv = mock(org.bukkit.inventory.Inventory.class);
    when(destInv.getHolder()).thenReturn(destState);
    when(destInv.getLocation()).thenReturn(destChestBlock.getLocation());

    InventoryMoveItemEvent event =
        new InventoryMoveItemEvent(sourceInv, new ItemStack(Material.DIAMOND), destInv, false);

    listener.onInventoryMove(event);

    assertFalse(event.isCancelled());
  }

  @Test
  public void testIsInventoryLocked() {
    Chest lockedChestState = (Chest) chestBlock.getState();
    Chest unprotectedChestState = (Chest) unprotectedChestBlock.getState();

    assertTrue(listener.isInventoryLocked(lockedChestState.getInventory()));
    assertFalse(listener.isInventoryLocked(unprotectedChestState.getInventory()));
  }
}
