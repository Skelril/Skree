/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.global.templeoffate;

import com.skelril.skree.SkreePlugin;
import com.skelril.skree.content.zone.LocationZone;
import com.skelril.skree.content.zone.ZoneNaturalSpawnBlocker;
import com.skelril.skree.service.internal.zone.ZoneRegion;
import com.skelril.skree.service.internal.zone.ZoneSpaceAllocator;
import com.skelril.skree.service.internal.zone.global.GlobalZoneManager;
import org.spongepowered.api.Sponge;

import java.util.function.Consumer;

public class TempleOfFateManager extends GlobalZoneManager<TempleOfFateInstance> implements LocationZone<TempleOfFateInstance> {

    public TempleOfFateManager() {
        Sponge.getEventManager().registerListeners(
                SkreePlugin.inst(),
                new TempleOfFateListener(this)
        );
        Sponge.getEventManager().registerListeners(
                SkreePlugin.inst(),
                new ZoneNaturalSpawnBlocker(a -> getApplicableZone(a).isPresent())
        );
    }

    @Override
    public void init(ZoneSpaceAllocator allocator, Consumer<TempleOfFateInstance> callback) {
        allocator.regionFor(getSystemName(), clause -> {
            ZoneRegion region = clause.getKey();

            TempleOfFateInstance instance = new TempleOfFateInstance(region);
            instance.init();

            callback.accept(instance);
        });
    }

    @Override
    public String getName() {
        return "Temple of Fate";
    }
}