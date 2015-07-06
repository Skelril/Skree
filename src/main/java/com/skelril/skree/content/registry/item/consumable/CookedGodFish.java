/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.item.consumable;

import com.skelril.nitro.registry.item.CookedItem;
import com.skelril.nitro.registry.item.CustomItem;
import com.skelril.skree.content.registry.item.CustomItemTypes;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CookedGodFish extends ItemFood implements CustomItem, CookedItem {
    public CookedGodFish() {
        super(10, 1F, false);
        maxStackSize = __getMaxStackSize();
        setCreativeTab(__getCreativeTab());
    }

    @Override
    public String __getID() {
        return "cooked_god_fish";
    }

    @Override
    public int __getMaxStackSize() {
        return 16;
    }

    @Override
    public CreativeTabs __getCreativeTab() {
        return CreativeTabs.tabFood;
    }

    @Override
    public void registerIngredients() {
        GameRegistry.addSmelting(new ItemStack(CustomItemTypes.RAW_GOD_FISH), new ItemStack(this), .45F);
    }
}
