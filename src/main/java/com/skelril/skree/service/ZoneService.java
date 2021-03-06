/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service;

import com.skelril.nitro.Clause;
import com.skelril.skree.service.internal.zone.Zone;
import com.skelril.skree.service.internal.zone.ZoneManager;
import com.skelril.skree.service.internal.zone.ZoneStatus;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public interface ZoneService {
  static String mangleManagerName(String managerName) {
    return managerName.toLowerCase().replace(" ", "");
  }

  void registerManager(ZoneManager<?> manager);

  Set<String> getManagerNames();

  Optional<Integer> getMaxGroupSize(String managerName);

  void requestZone(String managerName, Player player, Runnable preProcessCallback, Consumer<Optional<Clause<Player, ZoneStatus>>> callback);

  void requestZone(String managerName, Collection<Player> players, Runnable preProcessCallback, Consumer<Optional<Collection<Clause<Player, ZoneStatus>>>> callback);

  <T extends Zone> Optional<Integer> getMaxGroupSize(ZoneManager<T> manager);

  <T extends Zone> void requestZone(ZoneManager<T> manager, Player player, Runnable preProcessCallback, Consumer<Optional<Clause<Player, ZoneStatus>>> callback);

  <T extends Zone> void requestZone(ZoneManager<T> manager, Collection<Player> players, Runnable preProcessCallback, Consumer<Optional<Collection<Clause<Player, ZoneStatus>>>> callback);

  Clause<Player, ZoneStatus> rejoin(Player player);

  Collection<Clause<Player, ZoneStatus>> rejoin(Collection<Player> players);
}
