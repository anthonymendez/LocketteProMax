package me.crafter.mc.lockettepro.client;

import me.crafter.mc.lockettepro.LocketteProNetwork;

/**
 * Client-side Fabric Mod initializer for LocketteProMax.
 * Entrypoint referenced in fabric.mod.json when launched in a Fabric client environment.
 */
public class LocketteProClientMod {

  public static final String MOD_ID = "lockettepromax";

  public void onInitializeClient() {
    System.out.println("[LocketteProMax] Initializing Client Fabric Mod...");
    // Register client-side network payload listener and keybind handlers when running on Fabric Client
  }

  public static void handleIncomingPayload(byte[] data) {
    try {
      var lockInfos = LocketteProNetwork.decodePayload(data);
      LocketteClientCache.updateCache(lockInfos);
    } catch (Exception e) {
      System.err.println("[LocketteProMax] Failed to process lock status payload: " + e.getMessage());
    }
  }
}
