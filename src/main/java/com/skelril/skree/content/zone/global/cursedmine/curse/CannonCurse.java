/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.global.cursedmine.curse;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;

import java.util.function.Consumer;

public class CannonCurse implements Consumer<Player> {
  @Override
  public void accept(Player player) {
    Entity entity = player.getWorld().createEntity(EntityTypes.FIREBALL, player.getLocation().getPosition());
    player.getWorld().spawnEntity(entity);
  }
}
