/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service.internal.zone.global;

import com.skelril.skree.service.internal.zone.Zone;
import com.skelril.skree.service.internal.zone.ZoneManager;
import com.skelril.skree.service.internal.zone.ZoneSpaceAllocator;

import java.util.*;
import java.util.function.Consumer;

public abstract class GlobalZoneManager<T extends Zone> implements ZoneManager<T> {
  protected T zone;
  protected Queue<Consumer<Optional<T>>> pendingCallbacks = new ArrayDeque<>();

  public abstract void init(ZoneSpaceAllocator allocator, Consumer<T> callback);

  public boolean isActive() {
    return zone != null && zone.isActive();
  }

  @Override
  public void discover(ZoneSpaceAllocator allocator, Consumer<Optional<T>> callback) {
    if (!isActive()) {
      pendingCallbacks.add(callback);
      if (pendingCallbacks.size() == 1) {
        init(allocator, returnedZone -> {
          while (!pendingCallbacks.isEmpty()) {
            pendingCallbacks.poll().accept(Optional.of(zone = returnedZone));
          }
        });
      }
    } else {
      callback.accept(Optional.of(zone));
    }
  }

  public Optional<T> getActiveZone() {
    return Optional.ofNullable(zone);
  }

  @Override
  public Collection<T> getActiveZones() {
    return isActive() ? Collections.singletonList(zone) : Collections.emptyList();
  }
}
