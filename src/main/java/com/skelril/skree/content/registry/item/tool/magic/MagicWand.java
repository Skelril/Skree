/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.item.tool.magic;

import com.skelril.nitro.registry.item.CustomItem;
import com.skelril.nitro.selector.EventAwareContent;
import com.skelril.skree.content.registry.block.CustomBlockTypes;
import net.minecraft.creativetab.CreativeTabs;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import static com.skelril.nitro.item.ItemStackFactory.newItemStack;

public class MagicWand extends CustomItem implements EventAwareContent {

  @Override
  public String __getId() {
    return "magic_wand";
  }

  @Override
  public int __getMaxStackSize() {
    return 1;
  }

  @Override
  public CreativeTabs __getCreativeTab() {
    return CreativeTabs.TOOLS;
  }

  @Listener
  public void onRightClick(InteractBlockEvent.Secondary.MainHand event, @First Player player) {
    boolean survival = player.get(Keys.GAME_MODE).orElse(GameModes.CREATIVE) == GameModes.SURVIVAL;

    Optional<ItemStack> optHeldItem = player.getItemInHand(HandTypes.MAIN_HAND);

    if (!optHeldItem.isPresent()) {
      return;
    }

    if (optHeldItem.get().getItem() != this) {
      return;
    }

    event.setUseBlockResult(Tristate.FALSE);

    Optional<Location<World>> optLoc = event.getTargetBlock().getLocation();

    if (!optLoc.isPresent()) {
      return;
    }

    Location<World> loc = optLoc.get();
    BlockType targetType = loc.getBlockType();

    if (targetType == CustomBlockTypes.MAGIC_LADDER) {
      MagicBlockStateHelper.startLadder(loc);
    } else if (targetType == CustomBlockTypes.MAGIC_PLATFORM) {
      MagicBlockStateHelper.startPlatform(loc);
    } else {
      return;
    }

    if (!survival) {
      MagicBlockStateHelper.resetCounts();
      return;
    }

    MagicBlockStateHelper.dropItems(loc, event.getCause());
  }
}