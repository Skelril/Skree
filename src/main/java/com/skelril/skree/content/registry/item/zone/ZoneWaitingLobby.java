/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.item.zone;

import com.google.common.collect.Lists;
import com.skelril.skree.service.PlayerStateService;
import com.skelril.skree.service.internal.playerstate.InventoryStorageStateException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Snowball;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Supplier;

import static com.skelril.nitro.item.ItemStackFactory.newItemStack;

public class ZoneWaitingLobby {
    private Map<Player, Integer> playingPlayers = new WeakHashMap<>();
    private Supplier<Location<World>> entryPointSupplier;

    public ZoneWaitingLobby(Supplier<Location<World>> entryPointSupplier) {
        this.entryPointSupplier = entryPointSupplier;
    }

    public void add(Player player) {
        Optional<PlayerStateService> optService = Sponge.getServiceManager().provide(PlayerStateService.class);
        if (optService.isPresent()) {
            PlayerStateService service = optService.get();
            try {
                service.storeInventory(player);
                service.releaseInventory(player);

                player.getInventory().clear();
                player.getInventory().offer(newItemStack(ItemTypes.COOKED_BEEF, 64));
            } catch (InventoryStorageStateException e) {
                e.printStackTrace();
            }
        }

        player.setLocation(entryPointSupplier.get());
        player.sendMessage(Text.of(TextColors.YELLOW, "Your instance is building..."));
        player.sendMessage(Text.of(TextColors.YELLOW, "You will automatically be teleported when it's finished."));
        player.sendMessage(Text.of(TextColors.YELLOW, "For now have a snowball fight!"));

        playingPlayers.put(player, 0);
    }

    public void remove(Collection<Player> players) {
        HashMap<Player, Integer> localCounts = new HashMap<>();
        for (Player player : players) {
            localCounts.put(player, playingPlayers.remove(player));
        }

        boolean hasSnowballs = false;
        for (int value : localCounts.values()) {
            if (value > 0) {
                hasSnowballs = true;
                break;
            }
        }

        List<Text> endMessage = new ArrayList<>();
        if (hasSnowballs) {
            List<Map.Entry<Player, Integer>> results = Lists.newArrayList(localCounts.entrySet());
            results.sort((a, b) -> b.getValue() - a.getValue());

            endMessage.add(Text.of(TextColors.GOLD, "Top Snowball Fight Scores: "));
            for (int i = 0; i < Math.min(results.size(), 3); ++i) {
                Map.Entry<Player, Integer> playerScore = results.get(i);
                endMessage.add(Text.of(TextColors.YELLOW, i + 1, ") ", playerScore.getKey().getName(), " - ", playerScore.getValue()));
            }
        }

        players.stream().forEach(p -> {
            if (!endMessage.isEmpty()) {
                p.sendMessages(endMessage);
            }
            restoreInventory(p);
        });
    }

    private void restoreInventory(Player player) {
        Optional<PlayerStateService> optService = Sponge.getServiceManager().provide(PlayerStateService.class);
        if (optService.isPresent()) {
            PlayerStateService service = optService.get();
            if (service.hasInventoryStored(player)) {
                try {
                    service.loadInventory(player);
                } catch (InventoryStorageStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean contains(Player player) {
        return playingPlayers.containsKey(player);
    }

    @Listener
    public void onBlockInteract(InteractBlockEvent event) {
        Optional<Player> optPlayer = event.getCause().first(Player.class);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();
            BlockType type = event.getTargetBlock().getState().getType();

            if (type == BlockTypes.SNOW_LAYER && contains(player)) {
                player.getInventory().offer(newItemStack(ItemTypes.SNOWBALL, 16));
            }
        }
    }

    @Listener
    public void onSnowballHit(CollideEntityEvent.Impact event) {
        Entity entity = event.getCause().first(Entity.class).get();
        if (!(entity instanceof Snowball)) {
            return;
        }

        ProjectileSource source = ((Snowball) entity).getShooter();
        if (!(source instanceof Player)) {
            return;
        }

        Player attacker = (Player) source;

        for (Entity anEntity : event.getEntities()) {
            if (anEntity instanceof Player) {
                Player defender = (Player) anEntity;

                playingPlayers.merge(attacker, 1, (a, b) -> a + b);
                attacker.sendMessage(ChatTypes.ACTION_BAR, Text.of("Total score: ", playingPlayers.get(attacker)));

                playingPlayers.merge(defender, 1, (a, b) -> a - b);
                defender.sendMessage(ChatTypes.ACTION_BAR, Text.of("Total score: ", playingPlayers.get(defender)));
            }
        }
    }
}