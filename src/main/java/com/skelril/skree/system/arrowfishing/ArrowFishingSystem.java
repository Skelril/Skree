/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.system.arrowfishing;

import com.skelril.skree.SkreePlugin;
import com.skelril.skree.content.arrowfishing.ArrowFishingHandler;
import org.spongepowered.api.Sponge;

public class ArrowFishingSystem {
    public ArrowFishingSystem() {
        ArrowFishingHandler fishing = new ArrowFishingHandler();
        Sponge.getEventManager().registerListeners(SkreePlugin.inst(), fishing);
    }
}
