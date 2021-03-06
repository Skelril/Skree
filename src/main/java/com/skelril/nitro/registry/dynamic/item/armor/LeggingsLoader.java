/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.nitro.registry.dynamic.item.armor;

import com.skelril.nitro.registry.dynamic.ability.AbilityRegistry;
import com.skelril.nitro.registry.dynamic.item.GameIntegrator;
import com.skelril.nitro.registry.dynamic.item.ItemLoader;

public class LeggingsLoader extends ItemLoader<LoadedLeggings, LeggingsConfig> {
  public LeggingsLoader(GameIntegrator integrator, AbilityRegistry registry) {
    super(integrator, registry);
  }

  @Override
  public LoadedLeggings constructFromConfig(LeggingsConfig config) {
    return new LoadedLeggings(config);
  }

  @Override
  public Class<LeggingsConfig> getConfigClass() {
    return LeggingsConfig.class;
  }
}
