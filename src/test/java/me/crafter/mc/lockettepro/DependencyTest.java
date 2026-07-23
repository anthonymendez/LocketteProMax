package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DependencyTest extends LocketteProTestBase {

  private Player player;
  private Block block;

  @BeforeEach
  public void setUpDependency() {
    new Dependency(plugin);
    player = server.addPlayer("Alice");
    block = server.getWorld("world").getBlockAt(0, 64, 0);
    block.setType(Material.STONE);
  }

  @Test
  public void testIsProtectedFromReturnsFalseWithoutWorldGuard() {
    assertFalse(Dependency.isProtectedFrom(block, player));
  }

  @Test
  public void testIsPermissionGroupOfReturnsFalseWithoutVault() {
    assertFalse(Dependency.isPermissionGroupOf("[Admin]", player));
  }

  @Test
  public void testIsScoreboardTeamOfMatchesTeam() {
    Scoreboard scoreboard = server.getScoreboardManager().getMainScoreboard();
    Team team = scoreboard.registerNewTeam("RedTeam");
    team.addEntry(player.getName());

    assertTrue(Dependency.isScoreboardTeamOf("[RedTeam]", player));
    assertFalse(Dependency.isScoreboardTeamOf("[BlueTeam]", player));
  }

  @Test
  public void testLogPlacementSafeWithoutCoreProtect() {
    // Should execute silently without throwing NPE
    Dependency.logPlacement(player, block);
  }
}
