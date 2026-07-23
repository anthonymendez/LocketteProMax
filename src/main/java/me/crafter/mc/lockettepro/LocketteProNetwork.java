package me.crafter.mc.lockettepro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles custom payload network communication between LocketteProMax server plugin
 * and client-side Fabric mod instances.
 */
public class LocketteProNetwork {

  public static final String CHANNEL_NAME = "lockettepromax:lock_status";

  public enum LockStatus {
    UNLOCKED((byte) 0),
    LOCKED_ALLOWED((byte) 1),
    LOCKED_DENIED((byte) 2);

    private final byte code;

    LockStatus(byte code) {
      this.code = code;
    }

    public byte getCode() {
      return code;
    }

    public static LockStatus fromCode(byte code) {
      for (LockStatus status : values()) {
        if (status.code == code) {
          return status;
        }
      }
      return UNLOCKED;
    }
  }

  public static class BlockLockInfo {
    private final int x;
    private final int y;
    private final int z;
    private final LockStatus status;
    private final String owner;

    public BlockLockInfo(int x, int y, int z, LockStatus status, String owner) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.status = Objects.requireNonNullElse(status, LockStatus.UNLOCKED);
      this.owner = Objects.requireNonNullElse(owner, "");
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public int getZ() {
      return z;
    }

    public LockStatus getStatus() {
      return status;
    }

    public String getOwner() {
      return owner;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BlockLockInfo that = (BlockLockInfo) o;
      return x == that.x && y == that.y && z == that.z && status == that.status && Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, y, z, status, owner);
    }
  }

  /**
   * Registers outgoing and incoming plugin channel messaging with Bukkit messenger.
   */
  public static void registerNetwork(Plugin plugin) {
    if (plugin == null) return;
    try {
      Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_NAME);
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to register outgoing plugin channel: " + e.getMessage());
    }
  }

  /**
   * Unregisters outgoing plugin channel messaging with Bukkit messenger.
   */
  public static void unregisterNetwork(Plugin plugin) {
    if (plugin == null) return;
    try {
      Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_NAME);
    } catch (Exception e) {
      // Channel may already be unregistered
    }
  }

  /**
   * Serializes a list of BlockLockInfo entries into a byte array payload.
   */
  public static byte[] encodePayload(List<BlockLockInfo> list) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);

    dos.writeInt(list.size());
    for (BlockLockInfo info : list) {
      dos.writeInt(info.getX());
      dos.writeInt(info.getY());
      dos.writeInt(info.getZ());
      dos.writeByte(info.getStatus().getCode());
      dos.writeUTF(info.getOwner());
    }
    dos.flush();
    return baos.toByteArray();
  }

  /**
   * Deserializes a byte array payload into a list of BlockLockInfo entries.
   */
  public static List<BlockLockInfo> decodePayload(byte[] data) throws IOException {
    List<BlockLockInfo> list = new ArrayList<>();
    if (data == null || data.length < 4) {
      return list;
    }
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    DataInputStream dis = new DataInputStream(bais);

    int count = dis.readInt();
    for (int i = 0; i < count; i++) {
      int x = dis.readInt();
      int y = dis.readInt();
      int z = dis.readInt();
      byte statusCode = dis.readByte();
      String owner = dis.readUTF();
      list.add(new BlockLockInfo(x, y, z, LockStatus.fromCode(statusCode), owner));
    }
    return list;
  }

  /**
   * Computes lock status for a given block relative to a player.
   */
  public static LockStatus getBlockLockStatus(Block block, Player player) {
    if (block == null || !LocketteProAPI.isLockable(block)) {
      return LockStatus.UNLOCKED;
    }
    if (!LocketteProAPI.isLocked(block)) {
      return LockStatus.UNLOCKED;
    }
    if (player != null && (LocketteProAPI.isOwner(block, player) || LocketteProAPI.isUser(block, player))) {
      return LockStatus.LOCKED_ALLOWED;
    }
    return LockStatus.LOCKED_DENIED;
  }

  /**
   * Sends lock status packet for a list of blocks to a target player.
   */
  public static void sendLockStatus(Plugin plugin, Player player, List<Block> blocks) {
    if (plugin == null || player == null || !player.isOnline() || blocks == null || blocks.isEmpty()) {
      return;
    }
    List<BlockLockInfo> infoList = new ArrayList<>();
    for (Block b : blocks) {
      if (b == null) continue;
      LockStatus status = getBlockLockStatus(b, player);
      String owner = LocketteProAPI.getOwner(b);
      infoList.add(new BlockLockInfo(b.getX(), b.getY(), b.getZ(), status, owner != null ? owner : ""));
    }
    if (infoList.isEmpty()) {
      return;
    }

    try {
      byte[] payload = encodePayload(infoList);
      player.sendPluginMessage(plugin, CHANNEL_NAME, payload);
    } catch (Exception e) {
      // Player might not have channel registered or error sending payload
    }
  }
}
