package me.crafter.mc.lockettepro;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Utils {

    public static final String USERNAME_REGEX_PATTERN = "^[a-zA-Z0-9_]*$";

    private static final Cache<UUID, Block> selectedSign = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(30))
            .build();
    private static final Set<UUID> notified = new HashSet<>();
    private static final Map<Location, CacheEntry> blockCache = new ConcurrentHashMap<>();

    // Replaces the deprecated Bukkit Metadata API
    // (MetadataValue/FixedMetadataValue) to avoid scheduled deprecation
    // removals, eliminate metadata lookup overhead, and provide a lightweight,
    // type-safe in-memory cache.
    private static class CacheEntry {
        final long expires;
        final boolean locked;

        CacheEntry(long expires, boolean locked) {
            this.expires = expires;
            this.locked = locked;
        }
    }

    /**
     * Get a sign from a block.
     * 
     * @param block The block to get the sign from.
     * @return An optional containing the sign if the block is a sign, empty
     *         otherwise.
     */
    public static Optional<Sign> getSignFromBlock(Block block) {
        if (block.getState() instanceof Sign sign) {
            return Optional.of(sign);
        }
        return Optional.empty();
    }

    public static void updateSign(Block block) {
        // TODO: Add logging statement here.
        getSignFromBlock(block).ifPresentOrElse(sign -> sign.update(),
                () -> System.out.println("[Debug] Block is not a sign. Convert this to a proper log later."));

    }

    /**
     * Get the text from the given line on the front of the sign.
     * 
     * @param sign The sign to get the line from.
     * @param line The line to get (0-3).
     * @return The text from the given line on the front of the sign.
     */
    public static String getSignLine(Sign sign, int line) {
        return LegacyComponentSerializer.legacySection().serialize(sign.getSide(Side.FRONT).line(line));
    }

    /**
     * Get the text from all lines on the front of the sign.
     * 
     * @param sign The sign to get the lines from.
     * @return The text from all lines on the front of the sign.
     */
    public static String[] getSignLines(Sign sign) {
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = getSignLine(sign, i);
        }
        return lines;
    }

    /**
     * Set the text on the given line on the front of the sign.
     * 
     * @param sign   The sign to set the line on.
     * @param line   The line to set (0-3).
     * @param text   The text to set.
     * @param update Whether to update the sign.
     */
    public static void setSignLine(Sign sign, int line, String text, boolean update) {
        sign.getSide(Side.FRONT).line(line,
                LegacyComponentSerializer.legacySection().deserialize(text != null ? text : ""));
        if (update) {
            sign.update();
        }
    }

    /**
     * Set the text on the given line on the front of the sign.
     * 
     * Note: Does not update the sign by default.
     * 
     * @param sign The sign to set the line on.
     * @param line The line to set (0-3).
     * @param text The text to set.
     */
    public static void setSignLine(Sign sign, int line, String text) {
        setSignLine(sign, line, text, false);
    }

    /**
     * Set the text on the given line on the front of the sign.
     * Note: Checks if the block is a sign first. Use getSignFromBlock to get a
     * sign.
     * 
     * @param block  The block to set the line on.
     * @param line   The line to set (0-3).
     * @param text   The text to set.
     * @param update Whether to update the sign.
     */
    public static void setSignLine(Block block, int line, String text, boolean update) {
        // TODO: Add logging statement
        getSignFromBlock(block).ifPresentOrElse(sign -> {
            setSignLine(sign, line, text, update);
        }, () -> System.out.println("[Debug] Block is not a sign. Convert this to a proper log later."));
    }

    /**
     * Set the text on the given line on the front of the sign.
     * Note: Checks if the block is a sign first. Use getSignFromBlock to get a
     * sign.
     * 
     * @param block The block to set the line on.
     * @param line  The line to set (0-3).
     * @param text  The text to set.
     */
    public static void setSignLine(Block block, int line, String text) {
        setSignLine(block, line, text, true);
    }

    /**
     * Set the color of the sign.
     * 
     * @param sign   The si gn to set the color of.
     * @param color  The color to set.
     * @param update Whether to update the sign.
     */
    public static void setSignColor(Sign sign, DyeColor color, boolean update) {
        sign.getSide(Side.FRONT).setColor(color);
        if (update) {
            sign.update();
        }
    }

    /**
     * Set the color of the sign.
     * 
     * Note: Does not update the sign by default.
     * 
     * @param sign  The sign to set the color of.
     * @param color The color to set.
     */
    public static void setSignColor(Sign sign, DyeColor color) {
        setSignColor(sign, color, false);
    }

    /**
     * Returns whether if the sign is dark-colored.
     * 
     * Note: Dark-colored signs are the following material:
     * - DARK_OAK_SIGN
     * - DARK_OAK_WALL_SIGN
     * - CRIMSON_SIGN
     * - CRIMSON_WALL_SIGN
     */
    public static boolean isSignDarkMaterial(Sign sign) {
        Material type = sign.getBlock().getType();
        switch (type) {
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
            case CRIMSON_SIGN:
            case CRIMSON_WALL_SIGN:
                return true;
            default:
                return false;
        }
    }

    /**
     * Put a sign on a block.
     * 
     * @param block     The block to put the sign on.
     * @param blockFace The face of the block to put the sign on.
     * @param line1     The first line of the sign.
     * @param line2     The second line of the sign.
     * @param material  The material of the sign.
     * @return The sign that was placed.
     */
    public static Block putSignOn(Block block, BlockFace blockFace, String line1, String line2, Material material) {
        Block newSign = block.getRelative(blockFace);
        // TODO: Figure out why we replace sign with wall sign here.
        Material blockType = Material.getMaterial(material.name().replace("_SIGN", "_WALL_SIGN"));
        if (blockType != null && Tag.WALL_SIGNS.isTagged(blockType)) {
            newSign.setType(blockType);
        } else {
            newSign.setType(Material.OAK_WALL_SIGN);
        }
        BlockData data = newSign.getBlockData();
        if (data instanceof Directional) {
            ((Directional) data).setFacing(blockFace);
            newSign.setBlockData(data, true);
        }
        updateSign(newSign);
        Sign sign = (Sign) newSign.getState();
        if (isSignDarkMaterial(sign)) {
            setSignColor(sign, DyeColor.WHITE);
        }
        setSignLine(sign, 0, line1);
        setSignLine(sign, 1, line2);
        sign.update();
        return newSign;
    }

    /**
     * Remove a sign from the player's inventory.
     * 
     * @param player The player to remove the sign from.
     */
    public static void removeASign(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (mainHandItem.getAmount() == 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            mainHandItem.setAmount(mainHandItem.getAmount() - 1);
        }
    }

    /**
     * Get the selected sign.
     * 
     * @param player The player to get the selected sign from.
     * @return The selected sign.
     */
    public static Block getSelectedSign(Player player) {
        Block b = selectedSign.getIfPresent(player.getUniqueId());
        if (b != null && !player.getWorld().getName().equals(b.getWorld().getName())) {
            selectedSign.invalidate(player.getUniqueId());
            return null;
        }
        return b;
    }

    /**
     * Select a sign.
     * 
     * @param player The player to select the sign for.
     * @param block  The block to select.
     */
    public static void selectSign(Player player, Block block) {
        selectedSign.put(player.getUniqueId(), block);
    }

    /**
     * Play the lock effect.
     * 
     * @param player The player to play the lock effect for.
     * @param block  The block to play the lock effect at.
     */
    public static void playLockEffect(Player player, Block block) {
        // player.playSound(block.getLocation(), Sound.DOOR_CLOSE, 0.3F, 1.4F);
        // player.spigot().playEffect(block.getLocation().add(0.5, 0.5, 0.5),
        // Effect.CRIT, 0, 0, 0.3F, 0.3F, 0.3F, 0.1F, 64, 64);
    }

    /**
     * Play the access deny effect.
     * 
     * @param player The player to play the access deny effect for.
     * @param block  The block to play the access deny effect at.
     */
    public static void playAccessDenyEffect(Player player, Block block) {
        // player.playSound(block.getLocation(), Sound.VILLAGER_NO, 0.3F, 0.9F);
        // player.spigot().playEffect(block.getLocation().add(0.5, 0.5, 0.5),
        // Effect.FLAME, 0, 0, 0.3F, 0.3F, 0.3F, 0.01F, 64, 64);
    }

    /**
     * Send messages to a command sender.
     * 
     * @param sender   The command sender to send messages to.
     * @param messages The messages to send.
     */
    public static void sendMessages(CommandSender sender, String messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        sender.sendMessage(messages);
    }

    /**
     * Check if a player should be notified.
     * 
     * @param player The player to check.
     * @return Whether the player should be notified.
     */
    public static boolean shouldNotify(Player player) {
        if (notified.contains(player.getUniqueId())) {
            return false;
        } else {
            notified.add(player.getUniqueId());
            return true;
        }
    }

    /**
     * Check if a block has a valid cache.
     * 
     * Expires cache if it's too old.
     * 
     * @param block The block to check.
     * @return Whether the block has a valid cache.
     */
    public static boolean hasValidCache(Block block) {
        CacheEntry entry = blockCache.get(block.getLocation());
        return entry != null && entry.expires > System.currentTimeMillis();
    }

    /**
     * Gets the access status of a block.
     * 
     * @param block The block to get the access status of.
     * @return Whether the block is locked.
     */
    public static boolean getAccess(Block block) { // Requires hasValidCache()
        CacheEntry entry = blockCache.get(block.getLocation());
        return entry != null && entry.locked;
    }

    /**
     * Sets the cache for a block.
     * 
     * @param block  The block to set the cache for.
     * @param access Whether the block is locked.
     */
    public static void setCache(Block block, boolean access) {
        long expires = System.currentTimeMillis() + Config.getCacheTimeMillis();
        blockCache.put(block.getLocation(), new CacheEntry(expires, access));
    }

    /**
     * Resets the cache for a block.
     * 
     * @param block The block to reset the cache for.
     */
    public static void resetCache(Block block) {
        blockCache.remove(block.getLocation());
        for (BlockFace blockFace : LocketteProAPI.newsfaces) {
            Block relative = block.getRelative(blockFace);
            if (relative.getType() == block.getType()) {
                blockCache.remove(relative.getLocation());
            }
        }
    }

    /**
     * Update the UUID of the sign.
     * 
     * @param block The block to update the UUID of.
     */
    public static void updateUuidOnSign(Block block) {
        for (int line = 1; line < 4; line++) {
            updateUuidByUsername(block, line);
        }
    }

    /**
     * Update the UUID of a line on the sign.
     * 
     * @param block The block to update the UUID of.
     * @param line  The line to update the UUID of.
     */
    public static void updateUuidByUsername(final Block block, final int line) {
        Sign sign = (Sign) block.getState();
        final String original = getSignLine(sign, line);
        Bukkit.getScheduler().runTaskAsynchronously(LockettePro.getPlugin(), () -> {
            String username = original;
            if (username.contains("#")) {
                username = username.split("#")[0];
            }
            if (!isUserName(username)) {
                return;
            }
            String uuid = null;
            Player user = Bukkit.getPlayerExact(username);
            if (user != null) { // User is online
                uuid = user.getUniqueId().toString();
            } else { // User is not online, fetch string
                uuid = getUuidByUsernameFromMojang(username);
            }
            if (uuid != null) {
                final String toWrite = username + "#" + uuid;
                Bukkit.getScheduler().runTask(LockettePro.getPlugin(), () -> setSignLine(block, line, toWrite, true));
            }
        });
    }

    /**
     * Update the username of a line on the sign.
     * 
     * @param block The block to update the username of.
     * @param line  The line to update the username of.
     */
    public static void updateUsernameByUuid(Block block, int line) {
        Sign sign = (Sign) block.getState();
        String original = getSignLine(sign, line);
        if (isUsernameUuidLine(original)) {
            String uuid = getUuidFromLine(original);
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player != null) {
                setSignLine(block, line, player.getName() + "#" + uuid, true);
            }
        }
    }

    /**
     * Update a line on the sign with the player's name and UUID.
     * 
     * @param block  The block to update the line on.
     * @param line   The line to update.
     * @param player The player to update the line with.
     */
    public static void updateLineByPlayer(Block block, int line, Player player) {
        setSignLine(block, line, player.getName() + "#" + player.getUniqueId().toString(), true);
    }

    /**
     * Update a line on the sign with the time.
     * 
     * @param block    The block to update the line on.
     * @param noExpire Whether the line should not expire.
     */
    public static void updateLineWithTime(Block block, boolean noExpire) {
        Sign sign = (Sign) block.getState();
        if (noExpire) {
            setSignLine(sign, 0, getSignLine(sign, 0) + "#created:" + -1);
        } else {
            setSignLine(sign, 0, getSignLine(sign, 0) + "#created:" + (int) (System.currentTimeMillis() / 1000));
        }
        sign.update();
    }

    /**
     * Check if a string is a username.
     * 
     * @param text The string to check.
     * @return true if the string is a username, false otherwise.
     */
    public static boolean isUserName(String text) {
        return text.length() < 17 && text.length() > 2 && text.matches(USERNAME_REGEX_PATTERN);
    }

    /**
     * Get the UUID of a user by their username from Mojang.
     * <p>
     * <strong>Warning:</strong> Don't use this in a synchronous way.
     * </p>
     * 
     * @param username The username to get the UUID of.
     * @return The UUID of the user, or null if the user is not found.
     */
    public static String getUuidByUsernameFromMojang(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                String responseString = response.toString();
                JsonObject json = JsonParser.parseString(responseString).getAsJsonObject();
                String rawUuid = json.get("id").getAsString();
                return rawUuid.substring(0, 8) + "-" + rawUuid.substring(8, 12) + "-" + rawUuid.substring(12, 16) + "-"
                        + rawUuid.substring(16, 20) + "-" + rawUuid.substring(20);
            }
        } catch (Exception ignored) {
            // Ignored (e.g., offline or rate-limited API calls)
        }
        return null;
    }

    /**
     * Check if a string is a username-UUID line.
     * 
     * @param text The string to check.
     * @return true if the string is a username-UUID line, false otherwise.
     */
    public static boolean isUsernameUuidLine(String text) {
        if (text.contains("#")) {
            String[] parts = text.split("#", 2);
            return parts[1].length() == 36;
        }
        return false;
    }

    /**
     * Check if a string is a private time line.
     * 
     * @param text The string to check.
     * @return true if the string is a private time line, false otherwise.
     */
    public static boolean isPrivateTimeLine(String text) {
        if (text.contains("#")) {
            String[] parts = text.split("#", 2);
            return parts[1].startsWith("created:");
        }
        return false;
    }

    /**
     * Strip the sharp sign from a string.
     * 
     * @param text The string to strip the sharp sign from.
     * @return The string with the sharp sign stripped.
     */
    public static String stripSharpSign(String text) {
        if (text.contains("#")) {
            return text.split("#", 2)[0];
        } else {
            return text;
        }
    }

    /**
     * Get the username from a username-UUID line.
     * 
     * @param text The string to get the username from.
     * @return The username from the string.
     */
    public static String getUsernameFromLine(String text) {
        if (isUsernameUuidLine(text)) {
            return text.split("#", 2)[0];
        } else {
            return text;
        }
    }

    /**
     * Get the UUID from a username-UUID line.
     * 
     * @param text The string to get the UUID from.
     * @return The UUID from the string.
     */
    public static String getUuidFromLine(String text) {
        if (isUsernameUuidLine(text)) {
            return text.split("#", 2)[1];
        } else {
            return null;
        }
    }

    /**
     * Get the creation time from a private time line.
     * 
     * @param text The string to get the creation time from.
     * @return The creation time from the string.
     */
    public static long getCreatedFromLine(String text) {
        if (isPrivateTimeLine(text)) {
            return Long.parseLong(text.split("#created:", 2)[1]);
        } else {
            return Config.getLockDefaultCreateTimeUnix();
        }
    }

    /**
     * Check if a player is on a line.
     * 
     * @param player The player to check.
     * @param text   The string to check.
     * @return true if the player is on the line, false otherwise.
     */
    public static boolean isPlayerOnLine(Player player, String text) {
        if (Utils.isUsernameUuidLine(text)) {
            if (Config.isUuidEnabled()) {
                return player.getUniqueId().toString().equals(getUuidFromLine(text));
            } else {
                return player.getName().equals(getUsernameFromLine(text));
            }
        } else {
            return text.equals(player.getName());
        }
    }

    /**
     * Get the sign line from an unknown raw line.
     * 
     * @param rawLine The raw line to get the sign line from.
     * @return The sign line from the raw line.
     */
    public static String getSignLineFromUnknown(WrappedChatComponent rawLine) {
        String json = rawLine.getJson();
        return getSignLineFromUnknown(json);
    }

    /**
     * Get the sign line from an unknown JSON line.
     * 
     * @param json The JSON line to get the sign line from.
     * @return The sign line from the JSON line.
     */
    public static String getSignLineFromUnknown(String json) {
        try { // 1.9+
            if (json.length() > 33) {
                JsonObject line = JsonParser.parseString(json).getAsJsonObject();
                if (line.has("extra")) {
                    return line.get("extra").getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString();
                }
            }
            return "";
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return json;
    }

}
