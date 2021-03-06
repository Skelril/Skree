/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.world;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.world.biome.BiomeGenerationSettings;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.gen.WorldGenerator;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.gen.populator.Ore;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;

public class NoOreWorldGeneratorModifier implements WorldGeneratorModifier {
  @Override
  public void modifyWorldGenerator(WorldProperties world, DataContainer settings, WorldGenerator worldGenerator) {
    for (BiomeType biomeType : Sponge.getRegistry().getAllOf(BiomeType.class)) {
      BiomeGenerationSettings biomeData = worldGenerator.getBiomeSettings(biomeType);
      List<Ore> populators = biomeData.getPopulators(Ore.class);
      biomeData.getPopulators().removeAll(populators);
    }
  }

  @Override
  public String getId() {
    return "skree:no_ore";
  }

  @Override
  public String getName() {
    return "Ore Remover";
  }
}
