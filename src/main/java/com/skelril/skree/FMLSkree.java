/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * This "mod" exists solely due to the fact that the FMLInitializationEvent
 * differs from Sponge's InitializationEVent. Once this discrepancy is
 * resolved, the "Dirty Skree" mod can be removed from the project.
 */
@Mod(modid = "fml_skree", version = "1.0", name = "FML Skree")
public class FMLSkree {
  @Mod.EventHandler
  public void onInit(FMLInitializationEvent event) {
    SkreePlugin.inst().manager.trigger("FMLInitializationEvent");
  }
}
