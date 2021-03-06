/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service;

import com.skelril.skree.service.internal.region.Region;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public interface RegionService {
  Optional<Region> get(Location<World> location);

  Optional<Region> getOrCreate(Location<World> location, User user);

  Optional<Region> getMarkedRegion(Location<World> location);

  void rem(Location<World> location);

  int cleanup();

  void setSelectedRegion(Player player, Region region);

  Optional<Region> getSelectedRegion(Player player);
}
