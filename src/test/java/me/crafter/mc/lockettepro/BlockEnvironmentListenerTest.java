package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlockEnvironmentListenerTest extends LocketteProTestBase {

  private BlockEnvironmentListener listener;
  private Block chestBlock;
  private Block signBlock;
  private Block unprotectedBlock;
  private Player owner;

  @BeforeEach
  public void setUpListener() {
    listener = new BlockEnvironmentListener();
    owner = server.addPlayer("Alice");

    // Protected chest setup at (0, 64, 0)
    chestBlock = server.getWorld("world").getBlockAt(0, 64, 0);
    chestBlock.setType(Material.CHEST);

    // Sign attached to chest at (0, 64, -1) facing NORTH
    signBlock = server.getWorld("world").getBlockAt(0, 64, -1);
    signBlock.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) signBlock.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    signBlock.setBlockData(wallSignData);

    Utils.setSignLine(signBlock, 0, "[Private]", true);
    Utils.setSignLine(signBlock, 1, "Alice", true);

    // Unprotected block at (10, 64, 0)
    unprotectedBlock = server.getWorld("world").getBlockAt(10, 64, 0);
    unprotectedBlock.setType(Material.STONE);

    // Ensure protection configuration reset
    setExemptions(List.of("nothing"));
  }

  @AfterEach
  public void resetExemptions() {
    setExemptions(List.of("nothing"));
  }

  private void setExemptions(List<String> exemptions) {
    plugin.getConfig().set("protection-exempt", exemptions);
    plugin.saveConfig();
    Config.reload();
  }

  @Test
  public void testEntityExplodeRemovesProtectedBlock() {
    List<Block> blocks = new ArrayList<>(List.of(chestBlock, unprotectedBlock));
    EntityExplodeEvent event =
        new EntityExplodeEvent(
            mock(Entity.class), chestBlock.getLocation(), blocks, 0f, org.bukkit.ExplosionResult.DESTROY);

    listener.onEntityExplode(event);

    assertFalse(event.blockList().contains(chestBlock));
    assertTrue(event.blockList().contains(unprotectedBlock));
  }

  @Test
  public void testEntityExplodeExemptedDoesNotRemoveProtectedBlock() {
    setExemptions(List.of("explosion"));

    List<Block> blocks = new ArrayList<>(List.of(chestBlock, unprotectedBlock));
    EntityExplodeEvent event =
        new EntityExplodeEvent(
            mock(Entity.class), chestBlock.getLocation(), blocks, 0f, org.bukkit.ExplosionResult.DESTROY);

    listener.onEntityExplode(event);

    assertTrue(event.blockList().contains(chestBlock));
    assertTrue(event.blockList().contains(unprotectedBlock));
  }

  @Test
  public void testBlockExplodeRemovesProtectedBlock() {
    List<Block> blocks = new ArrayList<>(List.of(chestBlock, unprotectedBlock));
    BlockExplodeEvent event =
        new BlockExplodeEvent(
            unprotectedBlock, unprotectedBlock.getState(), blocks, 0f, org.bukkit.ExplosionResult.DESTROY);

    listener.onBlockExplode(event);

    assertFalse(event.blockList().contains(chestBlock));
    assertTrue(event.blockList().contains(unprotectedBlock));
  }

  @Test
  public void testBlockExplodeExemptedDoesNotRemoveProtectedBlock() {
    setExemptions(List.of("explosion"));

    List<Block> blocks = new ArrayList<>(List.of(chestBlock, unprotectedBlock));
    BlockExplodeEvent event =
        new BlockExplodeEvent(
            unprotectedBlock, unprotectedBlock.getState(), blocks, 0f, org.bukkit.ExplosionResult.DESTROY);

    listener.onBlockExplode(event);

    assertTrue(event.blockList().contains(chestBlock));
    assertTrue(event.blockList().contains(unprotectedBlock));
  }

  @Test
  public void testStructureGrowCancelledForProtectedBlock() {
    BlockState chestState = chestBlock.getState();
    StructureGrowEvent event =
        new StructureGrowEvent(
            chestBlock.getLocation(), TreeType.TREE, false, null, List.of(chestState));

    listener.onStructureGrow(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testStructureGrowNotCancelledWhenExemptedOrUnprotected() {
    BlockState stoneState = unprotectedBlock.getState();
    StructureGrowEvent event1 =
        new StructureGrowEvent(
            unprotectedBlock.getLocation(), TreeType.TREE, false, null, List.of(stoneState));

    listener.onStructureGrow(event1);
    assertFalse(event1.isCancelled());

    setExemptions(List.of("growth"));

    BlockState chestState = chestBlock.getState();
    StructureGrowEvent event2 =
        new StructureGrowEvent(
            chestBlock.getLocation(), TreeType.TREE, false, null, List.of(chestState));

    listener.onStructureGrow(event2);
    assertFalse(event2.isCancelled());
  }

  @Test
  public void testPistonExtendCancelledForProtectedBlock() {
    BlockPistonExtendEvent event =
        new BlockPistonExtendEvent(unprotectedBlock, List.of(chestBlock), BlockFace.NORTH);

    listener.onPistonExtend(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testPistonExtendNotCancelledWhenExemptedOrUnprotected() {
    BlockPistonExtendEvent event1 =
        new BlockPistonExtendEvent(unprotectedBlock, List.of(unprotectedBlock), BlockFace.NORTH);

    listener.onPistonExtend(event1);
    assertFalse(event1.isCancelled());

    setExemptions(List.of("piston"));

    BlockPistonExtendEvent event2 =
        new BlockPistonExtendEvent(unprotectedBlock, List.of(chestBlock), BlockFace.NORTH);

    listener.onPistonExtend(event2);
    assertFalse(event2.isCancelled());
  }

  @Test
  public void testPistonRetractCancelledForProtectedBlock() {
    BlockPistonRetractEvent event =
        new BlockPistonRetractEvent(unprotectedBlock, List.of(chestBlock), BlockFace.SOUTH);

    listener.onPistonRetract(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testPistonRetractNotCancelledWhenExemptedOrUnprotected() {
    BlockPistonRetractEvent event1 =
        new BlockPistonRetractEvent(unprotectedBlock, List.of(unprotectedBlock), BlockFace.SOUTH);

    listener.onPistonRetract(event1);
    assertFalse(event1.isCancelled());

    setExemptions(List.of("piston"));

    BlockPistonRetractEvent event2 =
        new BlockPistonRetractEvent(unprotectedBlock, List.of(chestBlock), BlockFace.SOUTH);

    listener.onPistonRetract(event2);
    assertFalse(event2.isCancelled());
  }

  @Test
  public void testRedstoneCurrentPreventedForProtectedBlock() {
    BlockRedstoneEvent event = new BlockRedstoneEvent(chestBlock, 0, 15);

    listener.onBlockRedstoneChange(event);

    assertEquals(0, event.getNewCurrent());
  }

  @Test
  public void testRedstoneCurrentAllowedWhenExemptedOrUnprotected() {
    BlockRedstoneEvent event1 = new BlockRedstoneEvent(unprotectedBlock, 0, 15);

    listener.onBlockRedstoneChange(event1);
    assertEquals(15, event1.getNewCurrent());

    setExemptions(List.of("redstone"));

    BlockRedstoneEvent event2 = new BlockRedstoneEvent(chestBlock, 0, 15);

    listener.onBlockRedstoneChange(event2);
    assertEquals(15, event2.getNewCurrent());
  }

  @Test
  public void testVillagerOpenDoorCancelledForProtectedDoor() {
    Block doorBlock = server.getWorld("world").getBlockAt(0, 64, 5);
    doorBlock.setType(Material.OAK_TRAPDOOR);

    Block doorSignBlock = server.getWorld("world").getBlockAt(0, 64, 4);
    doorSignBlock.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) doorSignBlock.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    doorSignBlock.setBlockData(wallSignData);

    Utils.setSignLine(doorSignBlock, 0, "[Private]", true);
    Utils.setSignLine(doorSignBlock, 1, "Alice", true);

    assertTrue(LocketteProAPI.isProtected(doorBlock));

    Villager villager = mock(Villager.class);
    EntityInteractEvent event = new EntityInteractEvent(villager, doorBlock);

    listener.onVillagerOpenDoor(event);

    assertTrue(event.isCancelled());
  }

  @Test
  public void testVillagerOpenDoorNotCancelledForChestOrNonVillagerOrExempted() {
    Villager villager = mock(Villager.class);

    // Protected chest is not a door
    EntityInteractEvent chestEvent = new EntityInteractEvent(villager, chestBlock);
    listener.onVillagerOpenDoor(chestEvent);
    assertFalse(chestEvent.isCancelled());

    // Setup protected door
    Block doorBlock = server.getWorld("world").getBlockAt(0, 64, 5);
    doorBlock.setType(Material.OAK_TRAPDOOR);
    Block doorSignBlock = server.getWorld("world").getBlockAt(0, 64, 4);
    doorSignBlock.setType(Material.OAK_WALL_SIGN);
    WallSign wallSignData = (WallSign) doorSignBlock.getBlockData();
    wallSignData.setFacing(BlockFace.NORTH);
    doorSignBlock.setBlockData(wallSignData);
    Utils.setSignLine(doorSignBlock, 0, "[Private]", true);
    Utils.setSignLine(doorSignBlock, 1, "Alice", true);

    // Non-villager entity (e.g. Zombie)
    Zombie zombie = mock(Zombie.class);
    EntityInteractEvent zombieEvent = new EntityInteractEvent(zombie, doorBlock);
    listener.onVillagerOpenDoor(zombieEvent);
    assertFalse(zombieEvent.isCancelled());

    // Villager when exempted
    setExemptions(List.of("villager"));
    EntityInteractEvent exemptedEvent = new EntityInteractEvent(villager, doorBlock);
    listener.onVillagerOpenDoor(exemptedEvent);
    assertFalse(exemptedEvent.isCancelled());
  }

  @Test
  public void testMobChangeBlockCancelledForTargetMobs() {
    Enderman enderman = mock(Enderman.class);
    EntityChangeBlockEvent endermanEvent =
        new EntityChangeBlockEvent(enderman, chestBlock, mock(BlockData.class));
    listener.onMobChangeBlock(endermanEvent);
    assertTrue(endermanEvent.isCancelled());

    Wither wither = mock(Wither.class);
    EntityChangeBlockEvent witherEvent =
        new EntityChangeBlockEvent(wither, chestBlock, mock(BlockData.class));
    listener.onMobChangeBlock(witherEvent);
    assertTrue(witherEvent.isCancelled());

    Zombie zombie = mock(Zombie.class);
    EntityChangeBlockEvent zombieEvent =
        new EntityChangeBlockEvent(zombie, chestBlock, mock(BlockData.class));
    listener.onMobChangeBlock(zombieEvent);
    assertTrue(zombieEvent.isCancelled());

    Silverfish silverfish = mock(Silverfish.class);
    EntityChangeBlockEvent silverfishEvent =
        new EntityChangeBlockEvent(silverfish, chestBlock, mock(BlockData.class));
    listener.onMobChangeBlock(silverfishEvent);
    assertTrue(silverfishEvent.isCancelled());
  }

  @Test
  public void testMobChangeBlockNotCancelledWhenExemptedOrOtherEntity() {
    Enderman enderman = mock(Enderman.class);

    // Unprotected block
    EntityChangeBlockEvent unprotectedEvent =
        new EntityChangeBlockEvent(enderman, unprotectedBlock, mock(BlockData.class));
    listener.onMobChangeBlock(unprotectedEvent);
    assertFalse(unprotectedEvent.isCancelled());

    // Exempted enderman
    setExemptions(List.of("enderman"));
    EntityChangeBlockEvent exemptedEvent =
        new EntityChangeBlockEvent(enderman, chestBlock, mock(BlockData.class));
    listener.onMobChangeBlock(exemptedEvent);
    assertFalse(exemptedEvent.isCancelled());

    // Non-target mob (e.g. Villager)
    Villager villager = mock(Villager.class);
    EntityChangeBlockEvent nonTargetEvent =
        new EntityChangeBlockEvent(villager, chestBlock, mock(BlockData.class));
    listener.onMobChangeBlock(nonTargetEvent);
    assertFalse(nonTargetEvent.isCancelled());
  }
}
