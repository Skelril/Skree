/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.nitro.registry.dynamic.ability;

import org.spongepowered.api.entity.living.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AbilityCooldownManager {
  private Map<UUID, Map<String, Long>> coolDownLookup = new HashMap<>();

  public void usedAbility(Player player, AbilityCooldownProfile abilityCooldown) {
    if (!abilityCooldown.isEnforced()) {
      return;
    }

    coolDownLookup.putIfAbsent(player.getUniqueId(), new HashMap<>());
    long currentTime = System.currentTimeMillis();
    long delay = Math.round(TimeUnit.SECONDS.toMillis(1) * (abilityCooldown.getSeconds()));
    long nextUsage = currentTime + delay;
    coolDownLookup.get(player.getUniqueId()).put(abilityCooldown.getPool(), nextUsage);
  }

  public boolean canUseAbility(Player player, AbilityCooldownProfile abilityCooldownProfile) {
    if (!abilityCooldownProfile.isEnforced()) {
      return true;
    }

    Map<String, Long> personalCoolDownMapping = coolDownLookup.getOrDefault(player.getUniqueId(), new HashMap<>());
    long coolDownExpireTime = personalCoolDownMapping.getOrDefault(abilityCooldownProfile.getPool(), 0L);

    boolean isOnCooldown = System.currentTimeMillis() < coolDownExpireTime;

    boolean isAllowedOnCooldown = isOnCooldown && abilityCooldownProfile.isAllowedWhileOnCooldown();
    boolean isAllowedOffCooldown = !isOnCooldown && abilityCooldownProfile.isAllowedWhileOffCooldown();

    return isAllowedOnCooldown || isAllowedOffCooldown;
  }
}
