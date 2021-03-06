/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.nitro.registry.dynamic.seteffect;

import com.skelril.nitro.registry.dynamic.ability.AbilityGroup;

import java.util.ArrayList;
import java.util.List;

public class SetEffectConfig {
  private List<AbilityGroup> abilityGroups = new ArrayList<>();

  public List<AbilityGroup> getAbilityGroups() {
    return abilityGroups;
  }
}
