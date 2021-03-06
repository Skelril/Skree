/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.item.tool;

import com.skelril.nitro.combat.PlayerCombatParser;
import com.skelril.nitro.registry.item.CustomItem;
import com.skelril.nitro.selector.EventAwareContent;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

import static com.skelril.nitro.item.ItemStackFactory.newItemStack;

public class PactScroll extends CustomItem implements EventAwareContent {

  private Map<UUID, List<UUID>> pactMap = new WeakHashMap<>();

  @Override
  public String __getId() {
    return "pact_scroll";
  }

  @Override
  public int __getMaxStackSize() {
    return 1;
  }

  @Override
  public CreativeTabs __getCreativeTab() {
    return CreativeTabs.TOOLS;
  }

  private boolean hasPactScroll(Player player) {
    return player.getInventory().query((ItemType) this).size() >= 1;
  }

  private PlayerCombatParser createFor(Cancellable event) {
    return new PlayerCombatParser() {
      @Override
      public void processPvP(Player attacker, Player defender) {
        if (pactMap.getOrDefault(attacker.getUniqueId(), new ArrayList<>()).contains(defender.getUniqueId())) {
          if (hasPactScroll(attacker) && hasPactScroll(defender)) {
            event.setCancelled(true);
          }
        } else if (pactMap.getOrDefault(defender.getUniqueId(), new ArrayList<>()).contains(attacker.getUniqueId())) {
          if (hasPactScroll(attacker) && hasPactScroll(defender)) {
            event.setCancelled(true);
            pactMap.get(defender.getUniqueId()).remove(attacker.getUniqueId());

            defender.sendMessage(Text.of(TextColors.DARK_RED, "Your pact with ", attacker.getName(), " has been broken!"));
          }
        }
      }
    };
  }

  @Listener(order = Order.PRE)
  public void onPlayerCombat(DamageEntityEvent event) {
    createFor(event).parse(event);
  }

  @Listener(order = Order.PRE)
  public void onPlayerCombat(CollideEntityEvent.Impact event) {
    createFor(event).parse(event);
  }

  @Listener
  public void onBlockInteract(InteractBlockEvent.Secondary.MainHand event, @Root Player player) {
    Optional<ItemStack> optItemStack = player.getItemInHand(HandTypes.MAIN_HAND);
    if (!optItemStack.isPresent()) {
      return;
    }

    ItemStack itemStack = optItemStack.get();
    if (itemStack.getItem() != this) {
      return;
    }

    UserStorageService userService = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
    PaginationService pagination = Sponge.getServiceManager().provideUnchecked(PaginationService.class);

    List<Text> result = pactMap.getOrDefault(player.getUniqueId(), new ArrayList<>()).stream()
        .map(userService::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .map(a -> Text.of((a.isOnline() ? TextColors.GREEN : TextColors.RED), a.getName()))
        .collect(Collectors.toList());

    pagination.builder()
        .contents(result)
        .title(Text.of(TextColors.GOLD, "Pact Members"))
        .padding(Text.of(" "))
        .sendTo(player);

    event.setUseBlockResult(Tristate.FALSE);
  }

  @Listener
  public void onEntityInteract(InteractEntityEvent.Primary event, @Root Player player) {
    Entity targetEntity = event.getTargetEntity();
    if (!(targetEntity instanceof Player)) {
      return;
    }
    Player targetPlayer = (Player) targetEntity;

    Optional<ItemStack> optItemStack = player.getItemInHand(HandTypes.MAIN_HAND);
    if (!optItemStack.isPresent()) {
      return;
    }

    ItemStack itemStack = optItemStack.get();
    if (itemStack.getItem() != this) {
      return;
    }

    pactMap.putIfAbsent(player.getUniqueId(), new ArrayList<>());
    List<UUID> pacts = pactMap.get(player.getUniqueId());
    if (pacts.contains(targetPlayer.getUniqueId())) {
      pacts.remove(targetPlayer.getUniqueId());
      player.sendMessage(Text.of(TextColors.YELLOW, "Your pact with ", targetPlayer.getName(), " has been broken!"));
    } else {
      pacts.add(targetPlayer.getUniqueId());
      player.sendMessage(Text.of(TextColors.YELLOW, "You've formed a pact with ", targetPlayer.getName(), "."));
      player.sendMessage(Text.of(TextColors.YELLOW, "You will no longer be able to damage ", targetPlayer.getName(), ", unless attacked first."));
      player.sendMessage(Text.of(TextColors.YELLOW, "You must carry a pact scroll for the pact to remain in effect."));
      player.sendMessage(Text.of(TextColors.YELLOW, "All pacts will automatically be reset upon disconnect."));
    }

    event.setCancelled(true);
  }

  @Listener
  public void onClientDisconnect(ClientConnectionEvent.Disconnect event) {
    pactMap.remove(event.getTargetEntity().getUniqueId());
  }
}

