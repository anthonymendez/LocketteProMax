package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

public class LocketteProTest extends LocketteProTestBase {

    // TODO: Separate assertions into multiple test methods
    @Test
    public void testChestLockingAndAccess() {
        // Create mock players
        Player owner = server.addPlayer("Alice");
        Player guest = server.addPlayer("Bob");
        Player stranger = server.addPlayer("Charlie");

        // Set up chest block at (0, 64, 0)
        Block chestBlock = server.getWorld("world").getBlockAt(0, 64, 0);
        chestBlock.setType(Material.CHEST);

        // Verify the chest is initially unlocked
        assertFalse(LocketteProAPI.isLocked(chestBlock));
        assertFalse(LocketteProAPI.isOwner(chestBlock, owner));

        // Set up oak wall sign block at (0, 64, -1) (NORTH of chest)
        Block signBlock = server.getWorld("world").getBlockAt(0, 64, -1);
        signBlock.setType(Material.OAK_WALL_SIGN);

        // Set the sign facing NORTH so it is recognized as attached to the chest
        WallSign wallSignData = (WallSign) signBlock.getBlockData();
        wallSignData.setFacing(BlockFace.NORTH);
        signBlock.setBlockData(wallSignData);

        // Set the sign lines to lock the chest for Alice (Owner)
        Utils.setSignLine(signBlock, 0, "[Private]", true);
        Utils.setSignLine(signBlock, 1, "Alice", true);

        // Verify that the sign is recognized as a lock sign
        assertTrue(LocketteProAPI.isSign(signBlock));
        assertTrue(LocketteProAPI.isLockSign(signBlock));

        // Verify that the chest is now locked
        assertTrue(LocketteProAPI.isLocked(chestBlock));

        // Verify Alice is the owner
        assertTrue(LocketteProAPI.isOwner(chestBlock, owner));
        assertTrue(LocketteProAPI.isUser(chestBlock, owner));

        // Verify Bob (guest) and Charlie (stranger) are locked out
        assertFalse(LocketteProAPI.isOwner(chestBlock, guest));
        assertFalse(LocketteProAPI.isUser(chestBlock, guest));
        assertFalse(LocketteProAPI.isOwner(chestBlock, stranger));
        assertFalse(LocketteProAPI.isUser(chestBlock, stranger));

        // Add Bob as an additional permitted user on line 3 (index 2) of the sign
        Utils.setSignLine(signBlock, 2, "Bob", true);

        // Verify Bob is now a permitted user but NOT the owner
        assertTrue(LocketteProAPI.isUser(chestBlock, guest));
        assertFalse(LocketteProAPI.isOwner(chestBlock, guest));

        // Verify Charlie is still completely locked out
        assertFalse(LocketteProAPI.isUser(chestBlock, stranger));
        assertFalse(LocketteProAPI.isOwner(chestBlock, stranger));
    }
}
