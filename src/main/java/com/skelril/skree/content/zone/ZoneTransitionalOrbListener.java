/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone;

import com.skelril.nitro.Clause;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.content.registry.item.CustomItemTypes;
import com.skelril.skree.service.internal.zone.Zone;
import com.skelril.skree.service.internal.zone.ZoneStatus;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.function.Function;

import static com.skelril.nitro.transformer.ForgeTransformer.tf;

public class ZoneTransitionalOrbListener<T extends Zone> extends ZoneApplicableListener<T> {
  public ZoneTransitionalOrbListener(Function<Location<World>, Optional<T>> applicabilityFunct) {
    super(applicabilityFunct);
  }

  @Listener
  public void onBlockInteract(InteractBlockEvent.Secondary.MainHand event, @First Player player) {
    Optional<org.spongepowered.api.item.inventory.ItemStack> optItemStack = player.getItemInHand(HandTypes.MAIN_HAND);
    if (!optItemStack.isPresent()) {
      return;
    }

    ItemStack itemStack = tf(optItemStack.get());
    if (itemStack.getItem() != CustomItemTypes.ZONE_TRANSITIONAL_ORB) {
      return;
    }

    Optional<T> optInst = getApplicable(player);
    if (optInst.isPresent()) {
      Clause<Player, ZoneStatus> status = optInst.get().remove(player);
      if (status.getValue() == ZoneStatus.REMOVED) {
        Task.builder().execute(() -> {
          tf(player).inventory.decrStackSize(tf(player).inventory.currentItem, 1);
          tf(player).inventoryContainer.detectAndSendChanges();
        }).delayTicks(1).submit(SkreePlugin.inst());
      }
      event.setUseBlockResult(Tristate.FALSE);
    }
  }
}
