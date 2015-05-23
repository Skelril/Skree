/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.system.registry.item;

import com.skelril.nitro.registry.item.CustomItem;
import com.skelril.skree.content.registry.item.admin.HackBook;
import com.skelril.skree.content.registry.item.weapon.sword.CrystalSword;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CustomItemSystem {
    public void init() {
        register(new HackBook());
        register(new CrystalSword());
    }

    private <T extends Item & CustomItem> T register(T item) {
        GameRegistry.registerItem(item, item.getID());
        return item;
    }
}