/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.group.jungleraid;

import com.skelril.nitro.Clause;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.content.zone.LocationZone;
import com.skelril.skree.content.zone.ZoneNaturalSpawnBlocker;
import com.skelril.skree.content.zone.ZoneTransitionalOrbListener;
import com.skelril.skree.service.internal.zone.ZoneRegion;
import com.skelril.skree.service.internal.zone.ZoneSpaceAllocator;
import com.skelril.skree.service.internal.zone.group.GroupZoneManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;

public class JungleRaidManager extends GroupZoneManager<JungleRaidInstance> implements Runnable, LocationZone<JungleRaidInstance> {
  private Queue<Consumer<Clause<ZoneRegion, ZoneRegion.State>>> pendingRequest = new ArrayDeque<>();
  private Queue<Clause<ZoneRegion, ZoneRegion.State>> finishedJobs = new ArrayDeque<>();
  private boolean jobInProgress = false;

  public JungleRaidManager() {
    Sponge.getEventManager().registerListeners(
        SkreePlugin.inst(),
        new JungleRaidListener(this)
    );
    Sponge.getEventManager().registerListeners(
        SkreePlugin.inst(),
        new JungleRaidEffectListener(this)
    );
    Sponge.getEventManager().registerListeners(
        SkreePlugin.inst(),
        new ZoneNaturalSpawnBlocker<>(this::getApplicableZone)
    );
    Sponge.getEventManager().registerListeners(
        SkreePlugin.inst(),
        new ZoneTransitionalOrbListener<>(this::getApplicableZone)
    );

    Task.builder().intervalTicks(20).execute(this).submit(SkreePlugin.inst());
  }

  private void processJobs(ZoneSpaceAllocator allocator) {
    // If there's a pending request, and finished job, use that area
    if (!pendingRequest.isEmpty()) {
      Clause<ZoneRegion, ZoneRegion.State> finishedJob = finishedJobs.poll();
      if (finishedJob != null) {
        pendingRequest.poll().accept(finishedJob);
      }
    }

    // If there are no finished jobs now, and there is not a job in progress
    // execute a new job
    if (finishedJobs.isEmpty() && !jobInProgress) {
      jobInProgress = true;

      // This is executed after the job completes, it adds the area to the finished jobs
      // reenables the creation of new jobs, and reprocesses the jobs
      Consumer<Clause<ZoneRegion, ZoneRegion.State>> recheck = a -> {
        finishedJobs.add(a);
        jobInProgress = false;
        processJobs(allocator);
      };

      allocator.regionFor(getSystemName(), recheck);
    }
  }

  @Override
  public void discover(ZoneSpaceAllocator allocator, Consumer<Optional<JungleRaidInstance>> callback) {
    pendingRequest.add(clause -> {
      ZoneRegion region = clause.getKey();

      // The schematic is 1 too high
      region = new ZoneRegion(allocator, region.getExtent(), region.getOrigin(), region.getBoundingBox().sub(0, 1, 0));

      JungleRaidInstance instance = new JungleRaidInstance(region);
      instance.init();
      zones.add(instance);

      callback.accept(Optional.of(instance));
    });

    processJobs(allocator);
  }

  @Override
  public String getName() {
    return "Jungle Raid";
  }

  @Override
  public void run() {
    Iterator<JungleRaidInstance> it = zones.iterator();
    while (it.hasNext()) {
      JungleRaidInstance next = it.next();
      if (next.isActive()) {
        next.run();
        continue;
      }
      next.forceEnd();

      Optional<ZoneSpaceAllocator> optAllocator = next.getRegion().getAllocator();
      if (optAllocator.isPresent()) {
        optAllocator.get().release(getSystemName(), next.getRegion());
      }

      it.remove();
    }
  }

}