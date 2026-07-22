package me.crafter.mc.lockettepro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.world.WorldMock;

public class UtilsTest extends LocketteProTestBase {

    private Block chestBlock;
    private Block signBlock;
    private Player player;

    @BeforeEach
    public void setUpUtils() {
        player = server.addPlayer("Alice");
        chestBlock = server.getWorld("world").getBlockAt(0, 64, 0);
        chestBlock.setType(Material.CHEST);
        signBlock = server.getWorld("world").getBlockAt(0, 64, -1);
        signBlock.setType(Material.OAK_WALL_SIGN);
        WallSign wallSignData = (WallSign) signBlock.getBlockData();
        wallSignData.setFacing(BlockFace.NORTH);
        signBlock.setBlockData(wallSignData);
    }

    @Test
    public void testGetSignFromBlockAndUpdateSign() {
        assertTrue(Utils.getSignFromBlock(signBlock).isPresent());
        assertFalse(Utils.getSignFromBlock(chestBlock).isPresent());

        Utils.updateSign(signBlock);
        Utils.updateSign(chestBlock); // non-sign block debug branch
    }

    @Test
    public void testSetAndGetSignLines() {
        Sign sign = (Sign) signBlock.getState();
        Utils.setSignLine(sign, 0, "[Private]", true);
        Utils.setSignLine(sign, 1, "Alice");
        assertEquals("[Private]", Utils.getSignLine(sign, 0));
        assertEquals("Alice", Utils.getSignLine(sign, 1));

        String[] lines = Utils.getSignLines(sign);
        assertEquals("[Private]", lines[0]);
        assertEquals("Alice", lines[1]);

        Utils.setSignLine(signBlock, 2, "Bob", true);
        Utils.setSignLine(signBlock, 3, "Charlie");
        assertEquals("Bob", Utils.getSignLine((Sign) signBlock.getState(), 2));
        assertEquals("Charlie", Utils.getSignLine((Sign) signBlock.getState(), 3));

        Utils.setSignLine(chestBlock, 0, "Test", true); // non-sign block debug branch
    }

    @Test
    public void testSetSignColorAndDarkMaterials() {
        Sign sign = (Sign) signBlock.getState();
        Utils.setSignColor(sign, DyeColor.RED, true);
        Utils.setSignColor(sign, DyeColor.BLUE);
        assertFalse(Utils.isSignDarkMaterial(sign));

        Block darkOakSignBlock = server.getWorld("world").getBlockAt(0, 64, 1);
        darkOakSignBlock.setType(Material.DARK_OAK_SIGN);
        Sign darkOakSign = (Sign) darkOakSignBlock.getState();
        assertTrue(Utils.isSignDarkMaterial(darkOakSign));

        Block crimsonSignBlock = server.getWorld("world").getBlockAt(0, 64, 2);
        crimsonSignBlock.setType(Material.CRIMSON_WALL_SIGN);
        Sign crimsonSign = (Sign) crimsonSignBlock.getState();
        assertTrue(Utils.isSignDarkMaterial(crimsonSign));
    }

    @Test
    public void testPutSignOn() {
        Block placedSignBlock = Utils.putSignOn(chestBlock, BlockFace.NORTH, "[Private]", "Alice", Material.OAK_SIGN);
        assertNotNull(placedSignBlock);
        assertTrue(placedSignBlock.getType() == Material.OAK_WALL_SIGN);
        Sign sign = (Sign) placedSignBlock.getState();
        assertEquals("[Private]", Utils.getSignLine(sign, 0));
        assertEquals("Alice", Utils.getSignLine(sign, 1));

        Block darkPlacedSignBlock = Utils.putSignOn(chestBlock, BlockFace.SOUTH, "[Private]", "Alice", Material.DARK_OAK_SIGN);
        assertNotNull(darkPlacedSignBlock);

        // Fallback else branch when material is not a sign
        Block fallbackSignBlock = Utils.putSignOn(chestBlock, BlockFace.WEST, "[Private]", "Alice", Material.DIAMOND);
        assertNotNull(fallbackSignBlock);
        assertEquals(Material.OAK_WALL_SIGN, fallbackSignBlock.getType());
    }

    @Test
    public void testRemoveASign() {
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().setItemInMainHand(new ItemStack(Material.OAK_SIGN, 1));
        Utils.removeASign(player);
        assertEquals(1, player.getInventory().getItemInMainHand().getAmount());

        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().setItemInMainHand(new ItemStack(Material.OAK_SIGN, 64));
        Utils.removeASign(player);
        assertEquals(63, player.getInventory().getItemInMainHand().getAmount());

        player.getInventory().setItemInMainHand(new ItemStack(Material.OAK_SIGN, 1));
        Utils.removeASign(player);
        assertEquals(Material.AIR, player.getInventory().getItemInMainHand().getType());
    }

    @Test
    public void testSelectSignAndWorldInvalidation() {
        assertNull(Utils.getSelectedSign(player));
        Utils.selectSign(player, signBlock);
        assertEquals(signBlock, Utils.getSelectedSign(player));

        // Player moves to another world
        WorldMock netherWorld = server.addSimpleWorld("world_nether");
        player.teleport(netherWorld.getSpawnLocation());
        assertNull(Utils.getSelectedSign(player));
    }

    @Test
    public void testPlayEffects() {
        Utils.playLockEffect(player, signBlock);
        Utils.playAccessDenyEffect(player, signBlock);
    }

    @Test
    public void testSendMessagesAndShouldNotify() {
        Utils.sendMessages(player, null);
        Utils.sendMessages(player, "");
        Utils.sendMessages(player, "Line 1\\nLine 2\nLine 3");

        assertTrue(Utils.shouldNotify(player));
        assertFalse(Utils.shouldNotify(player));
    }

    @Test
    public void testBlockCachingAndReset() {
        assertFalse(Utils.hasValidCache(chestBlock));
        assertFalse(Utils.getAccess(chestBlock));

        // Setup adjacent chest (double chest)
        Block chest2 = server.getWorld("world").getBlockAt(1, 64, 0);
        chest2.setType(Material.CHEST);

        Utils.setCache(chestBlock, true);
        Utils.setCache(chest2, true);
        assertTrue(Utils.getAccess(chestBlock));
        assertTrue(Utils.getAccess(chest2));

        // Reset cache on main chest should also reset adjacent chest
        Utils.resetCache(chestBlock);
        assertFalse(Utils.getAccess(chestBlock));
        assertFalse(Utils.getAccess(chest2));
    }

    @Test
    public void testUsernameValidationAndLineParsers() throws Exception {
        assertTrue(Utils.isUserName("Alice"));
        assertFalse(Utils.isUserName("A")); // too short (<3)
        assertFalse(Utils.isUserName("ThisIsTooLongOfAUsernameForMinecraft")); // too long (>16)
        assertFalse(Utils.isUserName("Invalid!Name")); // invalid regex chars

        String uuidStr = player.getUniqueId().toString();
        String uuidLine = "Alice#" + uuidStr;
        assertTrue(Utils.isUsernameUuidLine(uuidLine));
        assertFalse(Utils.isUsernameUuidLine("Alice#123"));

        String timeLine = "[Private]#created:123456789";
        assertTrue(Utils.isPrivateTimeLine(timeLine));
        assertFalse(Utils.isPrivateTimeLine("[Private]#somethingelse"));

        assertEquals("Alice", Utils.stripSharpSign(uuidLine));
        assertEquals("PlainString", Utils.stripSharpSign("PlainString"));

        assertEquals("Alice", Utils.getUsernameFromLine(uuidLine));
        assertEquals("PlainString", Utils.getUsernameFromLine("PlainString"));

        assertEquals(uuidStr, Utils.getUuidFromLine(uuidLine));
        assertNull(Utils.getUuidFromLine("PlainString"));

        assertEquals(123456789L, Utils.getCreatedFromLine(timeLine));
        assertEquals(Config.getLockDefaultCreateTimeUnix(), Utils.getCreatedFromLine("[Private]"));

        assertTrue(Utils.isPlayerOnLine(player, "Alice"));
        assertTrue(Utils.isPlayerOnLine(player, uuidLine));

        // Test isPlayerOnLine with Config.uuid enabled
        java.lang.reflect.Field field = Config.class.getDeclaredField("uuid");
        field.setAccessible(true);
        field.set(null, true);
        assertTrue(Utils.isPlayerOnLine(player, uuidLine));
        field.set(null, false);
    }

    @Test
    public void testUpdateLineHelpers() {
        Utils.updateLineByPlayer(signBlock, 1, player);
        String line1 = Utils.getSignLine((Sign) signBlock.getState(), 1);
        assertTrue(line1.startsWith("Alice#"));

        Utils.updateLineWithTime(signBlock, true);
        String line0NoExpire = Utils.getSignLine((Sign) signBlock.getState(), 0);
        assertTrue(line0NoExpire.contains("#created:-1"));

        Utils.updateLineWithTime(signBlock, false);
        String line0Expire = Utils.getSignLine((Sign) signBlock.getState(), 0);
        assertTrue(line0Expire.contains("#created:"));
    }

    @Test
    public void testAsyncUuidUpdateMethods() {
        Utils.setSignLine(signBlock, 1, "Alice");
        Utils.updateLineByPlayer(signBlock, 1, player);
        Utils.updateUsernameByUuid(signBlock, 1);
        Utils.updateUuidByUsername(signBlock, 1);
        Utils.updateUuidOnSign(signBlock);

        server.getScheduler().performOneTick();
        server.getScheduler().performTicks(10);
    }

    @Test
    public void testMojangApiFallback() {
        assertNull(Utils.getUuidByUsernameFromMojang("NonExistentUser123456789"));
    }

    @Test
    public void testUtilsConstructor() {
        assertNotNull(new Utils());
    }

    @Test
    public void testGetSignLineFromUnknown() {
        String jsonWithExtra = "{\"extra\":[{\"text\":\"Hello World\"}],\"text\":\"\"}";
        assertEquals("Hello World", Utils.getSignLineFromUnknown(jsonWithExtra));

        assertEquals("", Utils.getSignLineFromUnknown("ShortString"));
        assertEquals("", Utils.getSignLineFromUnknown("{\"text\":\"No extra field present in JSON\"}"));

        String invalidJsonLong = "This is an invalid JSON string that is over 33 characters long for testing";
        assertEquals(invalidJsonLong, Utils.getSignLineFromUnknown(invalidJsonLong));

        try {
            Utils.getSignLineFromUnknown((WrappedChatComponent) null);
        } catch (NullPointerException expected) {
            // Expected NullPointerException when extracting json from null wrapped component
        }
    }
}
