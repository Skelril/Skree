/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.item.tool;

import com.google.common.collect.ImmutableList;
import com.skelril.nitro.item.ItemCompactor;
import com.skelril.nitro.registry.item.CustomItem;
import com.skelril.nitro.selector.EventAwareContent;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.content.registry.item.currency.CofferValueMap;
import com.skelril.skree.content.registry.item.generic.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.NonNullList;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;

import static com.skelril.nitro.transformer.ForgeTransformer.tf;

public class ScrollOfSummation extends CustomItem implements EventAwareContent {

  @Override
  public String __getId() {
    return "scroll_of_summation";
  }

  @Override
  public int __getMaxStackSize() {
    return 64;
  }

  @Override
  public CreativeTabs __getCreativeTab() {
    return CreativeTabs.TOOLS;
  }

  @Listener
  public void onRightClick(InteractBlockEvent.Secondary.MainHand event, @First Player player) {
    Optional<ItemStack> optHeldItem = player.getItemInHand(HandTypes.MAIN_HAND);

    if (!optHeldItem.isPresent()) {
      return;
    }

    ItemStack held = optHeldItem.get();
    if (held.getItem() != this) {
      return;
    }

    NonNullList<net.minecraft.item.ItemStack> pInv = tf(player).inventory.mainInventory;
    //noinspection SuspiciousToArrayCall
    Optional<ItemStack[]> optCompacted = new ItemCompactor(ImmutableList.of(
        CoalValueMap.inst(),
        IronValueMap.inst(),
        GoldValueMap.inst(),
        RedstoneValueMap.inst(),
        LapisValueMap.inst(),
        DiamondValueMap.inst(),
        EmeraldValueMap.inst(),
        CofferValueMap.inst()
    )).execute(pInv.toArray(new ItemStack[pInv.size()]));

    if (optCompacted.isPresent()) {
      Task.builder().execute(() -> {
        ItemStack[] nInv = optCompacted.get();
        for (int i = 0; i < pInv.size(); ++i) {
          pInv.set(i, tf(nInv[i]));
        }
        tf(player).inventoryContainer.detectAndSendChanges();
        tf(player).inventory.decrStackSize(tf(player).inventory.currentItem, 1);
        player.sendMessage(Text.of(TextColors.GOLD, "The scroll glows brightly before turning to dust..."));
      }).delayTicks(1).submit(SkreePlugin.inst());

      event.setUseBlockResult(Tristate.FALSE);
    }
  }
}