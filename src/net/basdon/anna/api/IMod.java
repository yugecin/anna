// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

public interface IMod
{
/**
 * @return the mod name, should have a {@code mod_} prefix by convention
 */
String getName();
/**
 * @return version of the mod, freeform
 */
String getVersion();
/**
 * @return short description of what this mod does
 */
String getDescription();
/**
 * Called when this mod is being loaded.
 * @return {@code false} if it's not possible to enable this mod
 */
boolean onEnable();
/**
 * Called when this mod is being unloaded.
 * Remember to unregister every registered listeners etc.
 */
void onDisable();
void onRaw(String raw);
}
