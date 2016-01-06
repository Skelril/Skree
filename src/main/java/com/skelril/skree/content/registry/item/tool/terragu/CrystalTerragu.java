/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.item.tool.terragu;

import com.skelril.nitro.registry.HarvestTier;
import com.skelril.nitro.registry.item.HarvestTiers;
import com.skelril.skree.content.registry.item.CustomItemTypes;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;

public class CrystalTerragu extends CustomTerragu {
    @Override
    public String __getType() {
        return "crystal";
    }

    @Override
    public ItemStack __getRepairItemStack() {
        return new ItemStack(CustomItemTypes.SEA_CRYSTAL);
    }

    @Override
    public double __getHitPower() {
        return 6;
    }

    @Override
    public int __getEnchantability() {
        return ToolMaterial.EMERALD.getEnchantability();
    }

    @Override
    public HarvestTier __getHarvestTier() {
        return HarvestTiers.CRYSTAL;
    }

    @Override
    public float __getSpecializedSpeed() {
        return 10.0F;
    }

    @Override
    public int __getMaxUses() {
        return ToolMaterial.EMERALD.getMaxUses();
    }

    @Listener
    public void process(InteractBlockEvent.Primary event) {
        super.process(event);
    }

    @Listener
    public void process(InteractBlockEvent.Secondary event) {
        super.process(event);
    }

    @Listener
    public void process(ChangeBlockEvent.Break event) {
        super.process(event);
    }
}
