---
name: paper_sign_api
description: Best practices and gotchas for working with signs in Paper 26.2+, including SignChangeEvent handling on right-click, sign dye color bleed, and event-timing block updates.
---

# Paper 26.2+ Sign API Best Practices & Gotchas

## 1. Right-Clicking Signs & SignChangeEvent
In Minecraft 26.2+, right-clicking an existing sign opens the native sign editor UI. Closing this UI fires a `SignChangeEvent` even if the text wasn't modified.
- **Handling existing signs**: Distinguish between new sign placement and editing pre-existing signs using a guard condition.
- **Ownership validation**: Check ownership via direct line parsing (`isOwnerOnSign()`) rather than block-level state lookups when sign text may be in an invalid/error state.

## 2. Sign Dye Color & `getSignLine()` Color Bleed
When a legacy color code (like `§4`) or sign dye is applied to a sign side, `getSignLine()` returns legacy color-prefixed text (e.g. `§4[Private]`).
- **Always strip color codes**: Use `ChatColor.stripColor(...)` before comparing sign text against expected strings (e.g., `[Private]`, `[Everyone]`).

## 3. Block State Update Timing in `SignChangeEvent`
Modifying block state (such as setting sign dye color or updating sign data) directly inside a `SignChangeEvent` listener will be overwritten when the event finishes committing.
- **Schedule to next tick**: Use `Bukkit.getScheduler().runTask(plugin, () -> { ... })` to perform sign state/color updates after the `SignChangeEvent` has completed.
