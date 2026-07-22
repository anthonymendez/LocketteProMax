package me.crafter.mc.lockettepro;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlockPlayerListener implements Listener {

    // Quick protect for chests
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuickLockChest(PlayerInteractEvent event){
        // Check quick lock enabled
        if (Config.getQuickProtectAction() == (byte)0) return;
        // Get player and action info
        Action action = event.getAction();
        Player player = event.getPlayer();
        // Check action correctness
        if (action == Action.RIGHT_CLICK_BLOCK && Tag.SIGNS.isTagged(player.getInventory().getItemInMainHand().getType())) {
            if (player.getGameMode().equals(GameMode.SPECTATOR)) {
                return;
            }
            // Check quick lock action correctness
            if (!((event.getPlayer().isSneaking() && Config.getQuickProtectAction() == (byte)2) ||
                    (!event.getPlayer().isSneaking() && Config.getQuickProtectAction() == (byte)1))) return;
            // Check permission 
            if (!player.hasPermission(LockettePro.getPermission("lock"))) return;
            // Get target block to lock
            BlockFace blockface = event.getBlockFace();
            if (blockface == BlockFace.NORTH || blockface == BlockFace.WEST || blockface == BlockFace.EAST || blockface == BlockFace.SOUTH){
                Block block = event.getClickedBlock();
                if (block == null) return;
                // Check permission with external plugin
                if (Dependency.isProtectedFrom(block, player)) return; // blockwise
                if (Dependency.isProtectedFrom(block.getRelative(event.getBlockFace()), player)) return; // signwise
                // Check whether locking location is obstructed
                Block signLoc = block.getRelative(blockface);
                if (!signLoc.isEmpty()) return;
                // Check whether this block is lockable
                if (LocketteProAPI.isLockable(block)){
                    // Is this block already locked?
                    boolean locked = LocketteProAPI.isLocked(block);
                    // Cancel event here
                    event.setCancelled(true);
                    // Check lock info info
                    if (!locked && !LocketteProAPI.isUpDownLockedDoor(block)){
                    	Material signType = player.getInventory().getItemInMainHand().getType();
                        // Not locked, not a locked door nearby
                        Utils.removeASign(player);
                        // Send message
                        Utils.sendMessages(player, Config.getLang("locked-quick"));
                        // Put sign on
                        Block newsign = Utils.putSignOn(block, blockface, Config.getDefaultPrivateString(), player.getName(), signType);
                        Utils.resetCache(block);
                        // Cleanups - UUID
                        if (Config.isUuidEnabled()){
                            Utils.updateLineByPlayer(newsign, 1, player);
                        }
                        // Cleanups - Expiracy
                        if (Config.isLockExpire()) {
                            if (player.hasPermission(LockettePro.getPermission("noexpire"))) {
                                Utils.updateLineWithTime(newsign, true); // set created to -1 (no expire)
                            } else {
                                Utils.updateLineWithTime(newsign, false); // set created to now
                            }
                        }
                        Dependency.logPlacement(player, newsign);
                    } else if (!locked && LocketteProAPI.isOwnerUpDownLockedDoor(block, player)){
                        // Not locked, (is locked door nearby), is owner of locked door nearby
                        Material signType = player.getInventory().getItemInMainHand().getType();
                        Utils.removeASign(player);
                        Utils.sendMessages(player, Config.getLang("additional-sign-added-quick"));
                        Utils.putSignOn(block, blockface, Config.getDefaultAdditionalString(), "", signType);
                        Dependency.logPlacement(player, block.getRelative(blockface));
                    } else if (LocketteProAPI.isOwner(block, player)) {
                        // Locked, (not locked door nearby), is owner of locked block
                        Material signType = player.getInventory().getItemInMainHand().getType();
                        Utils.removeASign(player);
                        Utils.putSignOn(block, blockface, Config.getDefaultAdditionalString(), "", signType);
                        Utils.sendMessages(player, Config.getLang("additional-sign-added-quick"));
                        Dependency.logPlacement(player, block.getRelative(blockface));
                    } else {
                        // Cannot lock this block
                        Utils.sendMessages(player, Config.getLang("cannot-lock-quick"));
                    }
                }
            }
        }
    }
    
    // Manual protection
    @EventHandler(priority = EventPriority.NORMAL)
    public void onManualLock(SignChangeEvent event){
        if (!Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // --- Guard: editing a pre-existing lock/additional sign via the sign UI ---
        // In Minecraft 26.2+, right-clicking any existing sign fires a SignChangeEvent
        // when the player closes the editor. We must handle this case separately from
        // a player placing a brand-new sign.
        //
        // We detect "existing sign" by checking whether the attached block is already
        // locked AND this sign is one of its associated signs. We cannot rely on
        // isLockSignOrAdditionalSign(block) alone because the sign may have previously
        // had [ERROR] written to line 0, which would make that check fail.
        Block attachedForGuard = LocketteProAPI.getAttachedBlock(block);
        // Detect "editing existing sign" by checking the sign's own line 0, not the locked
        // state of the attached block — when [ERROR] is on line 0, isLocked() returns false.
        boolean isExistingSign = LocketteProAPI.isSign(block)
                && LocketteProAPI.isLockable(attachedForGuard)
                && (LocketteProAPI.isLockSignOrAdditionalSign(block)
                    || LocketteProAPI.isSignError(block));

        if (isExistingSign) {
            // Non-owner: restore original lines and send no-permission.
            // For signs in [ERROR] state, isOwnerOfSign() doesn't work (isLockSign fails),
            // so fall back to isOwnerOnSign() which reads line 1 directly.
            boolean isOwner = LocketteProAPI.isOwnerOfSign(block, player)
                    || LocketteProAPI.isOwnerOnSign(block, player)
                    || player.hasPermission(LockettePro.getPermission("admin.edit"));
            if (!isOwner) {
                String[] originalLines = Utils.getSignLines((Sign) block.getState());
                for (int i = 0; i < 4; i++) {
                    event.line(i, net.kyori.adventure.text.Component.text(originalLines[i]));
                }
                Utils.sendMessages(player, Config.getLang("no-permission"));
                return;
            }
            // Owner (or admin): validate line 0
            String newLine0 = event.getLine(0);
            if (newLine0 == null) newLine0 = "";
            if (!LocketteProAPI.isLockString(newLine0) && !LocketteProAPI.isAdditionalString(newLine0)) {
                // Invalid line 0 — write plain [ERROR] (no color code in the line text).
                // Paper interprets §4 in setLine() as a sign dye change to RED, which
                // then bleeds into future getSignLine() reads. We set the dye separately
                // on the next tick, after the event is committed to the block.
                event.setLine(0, "[ERROR]");
                Utils.sendMessages(player, Config.getLang("cannot-change-this-line"));
                Bukkit.getScheduler().runTask(LockettePro.getPlugin(), () -> {
                    Utils.getSignFromBlock(block).ifPresent(s -> Utils.setSignColor(s, DyeColor.RED, true));
                });
            } else {
                // Valid lock string — reset sign dye to default on the next tick,
                // after the event has been committed to the block.
                DyeColor defaultColor;
                {
                    Sign signState = (Sign) block.getState();
                    defaultColor = Utils.isSignDarkMaterial(signState) ? DyeColor.WHITE : DyeColor.BLACK;
                }
                final DyeColor resetColor = defaultColor;
                Bukkit.getScheduler().runTask(LockettePro.getPlugin(), () -> {
                    Utils.getSignFromBlock(block).ifPresent(s -> Utils.setSignColor(s, resetColor, true));
                });
            }
            Utils.resetCache(attachedForGuard);
            return;
        }


        // --- Original new-sign-placement logic (unchanged) ---
        String topline = event.getLine(0);
        if (topline == null) topline = "";
        /*  Issue #46 - Old version of Minecraft trim signs in unexpected way.
         *  This is caused by Minecraft was doing: (unconfirmed but seemingly)\
         *  Place Sign -> Event Fire -> Trim Sign
         *  The event.getLine() will be inaccurate if the line has white space to trim
         * 
         *  This will cause player without permission will be able to lock chests by
         *  adding a white space after the [private] word.
         *  Currently this is fixed by using trimmed line in checking permission. Trimmed
         *  line should not be used anywhere else.  
         */
        if (!player.hasPermission(LockettePro.getPermission("lock"))){
            String toplinetrimmed = topline.trim();
            if (LocketteProAPI.isLockString(toplinetrimmed) || LocketteProAPI.isAdditionalString(toplinetrimmed)){
                event.setLine(0, Config.getLang("sign-error"));
                Utils.sendMessages(player, Config.getLang("cannot-lock-manual"));
                return;
            }
        }
        if (LocketteProAPI.isLockString(topline) || LocketteProAPI.isAdditionalString(topline)){
            Block attachedBlock = LocketteProAPI.getAttachedBlock(event.getBlock());
            if (LocketteProAPI.isLockable(attachedBlock)){
                if (Dependency.isProtectedFrom(attachedBlock, player)){ // External check here
                    event.setLine(0, Config.getLang("sign-error"));
                    Utils.sendMessages(player, Config.getLang("cannot-lock-manual"));
                    return; 
                }
                boolean locked = LocketteProAPI.isLocked(attachedBlock);
                if (!locked && !LocketteProAPI.isUpDownLockedDoor(attachedBlock)){
                    if (LocketteProAPI.isLockString(topline)){
                        Utils.sendMessages(player, Config.getLang("locked-manual"));
                        if (!player.hasPermission(LockettePro.getPermission("lockothers"))){ // Player with permission can lock with another name
                            event.setLine(1, player.getName());
                        }
                        Utils.resetCache(attachedBlock);
                    } else {
                        Utils.sendMessages(player, Config.getLang("not-locked-yet-manual"));
                        event.setLine(0, Config.getLang("sign-error"));
                    }
                } else if (!locked && LocketteProAPI.isOwnerUpDownLockedDoor(attachedBlock, player)){
                    if (LocketteProAPI.isLockString(topline)){
                        Utils.sendMessages(player, Config.getLang("cannot-lock-door-nearby-manual"));
                        event.setLine(0, Config.getLang("sign-error"));
                    } else {
                        Utils.sendMessages(player, Config.getLang("additional-sign-added-manual"));
                    }
                } else if (LocketteProAPI.isOwner(attachedBlock, player)){
                    if (LocketteProAPI.isLockString(topline)){
                        Utils.sendMessages(player, Config.getLang("block-already-locked-manual"));
                        event.setLine(0, Config.getLang("sign-error"));
                    } else {
                        Utils.sendMessages(player, Config.getLang("additional-sign-added-manual"));
                    }
                } else { // Not possible to fall here except override
                    Utils.sendMessages(player, Config.getLang("block-already-locked-manual"));
                    event.getBlock().breakNaturally();
                    Utils.playAccessDenyEffect(player, attachedBlock);
                }
            } else {
                Utils.sendMessages(player, Config.getLang("block-is-not-lockable"));
                event.setLine(0, Config.getLang("sign-error"));
                Utils.playAccessDenyEffect(player, attachedBlock);
            }
        }
    }

    
    // Player select sign
    @EventHandler(priority = EventPriority.LOW)
    public void playerSelectSign(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && Tag.WALL_SIGNS.isTagged(block.getType())) {
            Player player = event.getPlayer();
            if (!player.hasPermission(LockettePro.getPermission("edit"))) return;
            if (LocketteProAPI.isOwnerOfSign(block, player) || (LocketteProAPI.isLockSignOrAdditionalSign(block) && player.hasPermission(LockettePro.getPermission("admin.edit")))){
                Utils.selectSign(player, block);
                Utils.sendMessages(player, Config.getLang("sign-selected"));
                Utils.playLockEffect(player, block);
            }
        }
    }
    
    // Player break sign
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttemptBreakSign(BlockBreakEvent event){
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.hasPermission(LockettePro.getPermission("admin.break"))) return;
        if (LocketteProAPI.isLockSign(block)){
            if (LocketteProAPI.isOwnerOfSign(block, player)){
                Utils.sendMessages(player, Config.getLang("break-own-lock-sign"));
                Utils.resetCache(LocketteProAPI.getAttachedBlock(block));
                // Remove additional signs?
            } else {
                Utils.sendMessages(player, Config.getLang("cannot-break-this-lock-sign"));
                event.setCancelled(true);
                Utils.playAccessDenyEffect(player, block);
            }
        } else if (LocketteProAPI.isAdditionalSign(block)){
            // TODO the next line is spaghetti
            if (!LocketteProAPI.isLocked(LocketteProAPI.getAttachedBlock(block))){
                // phew, the locked block is expired!
                // nothing
            } else if (LocketteProAPI.isOwnerOfSign(block, player)){
                Utils.sendMessages(player, Config.getLang("break-own-additional-sign"));
            } else if (!LocketteProAPI.isProtected(LocketteProAPI.getAttachedBlock(block))){
                Utils.sendMessages(player, Config.getLang("break-redundant-additional-sign"));
            } else {
                Utils.sendMessages(player, Config.getLang("cannot-break-this-additional-sign"));
                event.setCancelled(true);
                Utils.playAccessDenyEffect(player, block);
            }
        }
    }
    
    // Protect block from being destroyed
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttemptBreakLockedBlocks(BlockBreakEvent event){
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (LocketteProAPI.isLocked(block) || LocketteProAPI.isUpDownLockedDoor(block)){
            Utils.sendMessages(player, Config.getLang("block-is-locked"));
            event.setCancelled(true);
            Utils.playAccessDenyEffect(player, block);
        }
    }

    // Protect block from being used & handle double doors
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttemptInteractLockedBlocks(PlayerInteractEvent event) {
    	if (event.hasBlock() == false) return;
        if (Objects.equals(event.getHand(), EquipmentSlot.OFF_HAND)) return;
        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (LockettePro.needCheckHand() && LocketteProAPI.isChest(block)){
            if (event.getHand() != EquipmentSlot.HAND){
                if (action == Action.RIGHT_CLICK_BLOCK){
                    /*if (LocketteProAPI.isChest(block)){
                        // something not right
                        event.setCancelled(true);
                    }*/
                    event.setCancelled(true);
                    return;
                }
            }
        }
        switch (action){
        case LEFT_CLICK_BLOCK:
        case RIGHT_CLICK_BLOCK:
            Player player = event.getPlayer();
            if (((LocketteProAPI.isLocked(block) && !LocketteProAPI.isUser(block, player)) || (LocketteProAPI.isUpDownLockedDoor(block) && !LocketteProAPI.isUserUpDownLockedDoor(block, player))) && !player.hasPermission(LockettePro.getPermission("admin.use"))){
                Utils.sendMessages(player, Config.getLang("block-is-locked"));
                event.setCancelled(true);
                Utils.playAccessDenyEffect(player, block);
            } else { // Handle double doors
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    if ((LocketteProAPI.isDoubleDoorBlock(block) || LocketteProAPI.isSingleDoorBlock(block)) && LocketteProAPI.isLocked(block)){
                        Block doorblock = LocketteProAPI.getBottomDoorBlock(block);
                        org.bukkit.block.data.Openable openablestate = (org.bukkit.block.data.Openable ) doorblock.getBlockData();
                        boolean shouldopen = !openablestate.isOpen(); // Move to here
                        int closetime = LocketteProAPI.getTimerDoor(doorblock);
                        List<Block> doors = new ArrayList<Block>();
                        doors.add(doorblock);
                        if (doorblock.getType() == Material.IRON_DOOR || doorblock.getType() == Material.IRON_TRAPDOOR){
                            LocketteProAPI.toggleDoor(doorblock, shouldopen);
                        }
                        for (BlockFace blockface : LocketteProAPI.newsfaces){
                            Block relative = doorblock.getRelative(blockface);
                            if (relative.getType() == doorblock.getType()){
                                doors.add(relative);
                                LocketteProAPI.toggleDoor(relative, shouldopen);
                            }
                        }
                        if (closetime > 0) {
                            for (Block door : doors) {
                                if (door.hasMetadata("lockettepro.toggle")) {
                                    return;
                                }
                            }
                            for (Block door : doors) {
                                door.setMetadata("lockettepro.toggle", new FixedMetadataValue(LockettePro.getPlugin(), true));
                            }
                            Bukkit.getScheduler().runTaskLater(LockettePro.getPlugin(), new DoorToggleTask(doors), closetime*20);
                        }
                    }
                }
            }
            break;
        default:
            break;
        }
    }
    
    // Protect block from interfere block
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttemptPlaceInterfereBlocks(BlockPlaceEvent event){
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.hasPermission(LockettePro.getPermission("admin.interfere"))) return;
        if (LocketteProAPI.mayInterfere(block, player)){
            Utils.sendMessages(player, Config.getLang("cannot-interfere-with-others"));
            event.setCancelled(true);
            Utils.playAccessDenyEffect(player, block);		
        }
    }
    
    // Tell player about lockettepro
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlaceFirstBlockNotify(BlockPlaceEvent event){
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (!player.hasPermission(LockettePro.getPermission("lock"))) return;
        if (Utils.shouldNotify(player) && Config.isLockable(block.getType())){
            switch (Config.getQuickProtectAction()){
            case (byte)0:
                Utils.sendMessages(player, Config.getLang("you-can-manual-lock-it"));	
                break;
            case (byte)1:
            case (byte)2:
                Utils.sendMessages(player, Config.getLang("you-can-quick-lock-it"));	
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (LocketteProAPI.isProtected(block) && !(LocketteProAPI.isOwner(block, player) || LocketteProAPI.isOwnerOfSign(block, player))) {
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isDead()) {
                        player.updateInventory();
                    }
                }
            }.runTaskLater(LockettePro.getPlugin(), 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketUse(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (LocketteProAPI.isProtected(block) && !(LocketteProAPI.isOwner(block, player) || LocketteProAPI.isOwnerOfSign(block, player))) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onLecternTake(PlayerTakeLecternBookEvent event){
        Player player = event.getPlayer();
        Block block = event.getLectern().getBlock();
        if(LocketteProAPI.isProtected(block) && !(LocketteProAPI.isOwner(block, player) || LocketteProAPI.isOwnerOfSign(block, player))){
            event.setCancelled(true);
        }
    }
}
