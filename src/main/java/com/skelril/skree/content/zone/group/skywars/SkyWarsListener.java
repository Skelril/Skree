/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.group.skywars;

import com.flowpowered.math.vector.Vector3d;
import com.sk89q.worldedit.BlockWorldVector;
import com.sk89q.worldedit.sponge.SpongePlayer;
import com.sk89q.worldedit.sponge.SpongeWorldEdit;
import com.sk89q.worldedit.util.TargetBlock;
import com.skelril.nitro.combat.PlayerCombatParser;
import com.skelril.nitro.entity.EntityDirectionUtil;
import com.skelril.nitro.probability.Probability;
import com.skelril.skree.content.registry.item.CustomItemTypes;
import com.skelril.skree.content.registry.item.minigame.SkyFeather;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.animal.Chicken;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.Snowball;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;

import static com.skelril.nitro.transformer.ForgeTransformer.tf;

public class SkyWarsListener {
  private final SkyWarsManager manager;

  public SkyWarsListener(SkyWarsManager manager) {
    this.manager = manager;
  }

  @Listener
  public void onInteract(InteractBlockEvent event, @First Player player) {
    Optional<SkyWarsInstance> optInst = manager.getApplicableZone(player);
    if (!optInst.isPresent()) {
      return;
    }

    SkyWarsInstance inst = optInst.get();
    if (inst.getState() != SkyWarsState.IN_PROGRESS) {
      return;
    }

    Optional<SkyWarsPlayerData> optPlayerData = inst.getPlayerData(player);
    if (!optPlayerData.isPresent()) {
      return;
    }
    SkyWarsPlayerData playerData = optPlayerData.get();

    Optional<ItemStack> optStack = player.getItemInHand(HandTypes.MAIN_HAND);
    if (!optStack.isPresent()) {
      return;
    }
    ItemStack stack = optStack.get();

    if (stack.getType() == CustomItemTypes.SKY_FEATHER) {

      Vector3d vel = EntityDirectionUtil.getFacingVector(player);

      Optional<SkyFeather.Data> optData = SkyFeather.getDataFor(stack);
      if (!optData.isPresent()) {
        return;
      }

      SkyFeather.Data data = optData.get();

      double radius = data.radius;
      double flight = data.flight;
      double pushBack = data.pushBack;

      if (event instanceof InteractBlockEvent.Primary.MainHand) {
        if (!playerData.canFly()) {
          return;
        }

        vel = vel.mul(flight);
        player.setVelocity(vel);

        playerData.stopFlight(250);
      } else if (event instanceof InteractBlockEvent.Secondary.MainHand) {
        if (!playerData.canPushBack()) {
          return;
        }
        vel = vel.mul(pushBack * 2);

        SpongePlayer spongePlayer = SpongeWorldEdit.inst().wrapPlayer(player);

        Collection<Entity> possibleTargets = inst.getContained(Player.class, Chicken.class);
        possibleTargets.remove(player);

        ParticleEffect radiationEffect = ParticleEffect.builder().type(
            ParticleTypes.FLAME
        ).quantity(1).build();

        TargetBlock targetBlock = new TargetBlock(spongePlayer, 50, .2);
        while (targetBlock.getNextBlock() != null) {
          BlockWorldVector weBlock = targetBlock.getCurrentBlock();

          Location<World> loc = new Location<>(inst.getRegion().getExtent(), weBlock.getX(), weBlock.getY(), weBlock.getZ());
          for (int i = 0; i < 10; ++i) {
            inst.getRegion().getExtent().spawnParticles(radiationEffect, loc.getPosition().add(
                Probability.getRangedRandom(0, 1.0),
                Probability.getRangedRandom(0, 1.0),
                Probability.getRangedRandom(0, 1.0)
            ));
          }

          for (Entity aEntity : possibleTargets) {
            if (aEntity.getLocation().getPosition().distanceSquared(loc.getPosition()) <= Math.pow(radius, 2)) {
              if (aEntity instanceof Player) {
                Player aPlayer = (Player) aEntity;

                if (inst.isFriendlyFire(player, aPlayer)) {
                  continue;
                }

                // Handle Sender
                playerData.stopPushBack(250);
                player.sendMessage(Text.of(TextColors.YELLOW, "You push back: ", aPlayer.getName(), "!"));

                // Handle Target
                aPlayer.setVelocity(vel);

                Optional<SkyWarsPlayerData> optAPlayerData = inst.getPlayerData(aPlayer);
                if (optAPlayerData.isPresent()) {
                  SkyWarsPlayerData aPlayerData = optAPlayerData.get();
                  if (aPlayerData.canDefrost()) {
                    aPlayerData.stopFlight();
                  }
                }
              } else {
                inst.awardPowerup(player, stack);
                aEntity.remove();
              }
            }
          }
        }
      }

      // TODO: Port to Sponge and handle removal properly
      tf(stack).attemptDamageItem(1, new Random(), null);
    }
  }

  private PlayerCombatParser createFor(Cancellable event, SkyWarsInstance inst) {
    return new PlayerCombatParser() {
      @Override
      public void processPvP(Player attacker, Player defender, @Nullable Entity indirectSource) {
        if (inst.getState() != SkyWarsState.IN_PROGRESS) {
          attacker.sendMessage(Text.of(TextColors.RED, "You can't attack players right now!"));
          event.setCancelled(true);
          return;
        }

        if (inst.isFriendlyFire(attacker, defender)) {
          attacker.sendMessage(Text.of(TextColors.RED, "Don't hit your team mates!"));
          event.setCancelled(true);
          return;
        }

        if (indirectSource instanceof Snowball && event instanceof CollideEntityEvent.Impact) {
          Optional<SkyWarsPlayerData> optData = inst.getPlayerData(defender);
          if (optData.isPresent()) {
            SkyWarsPlayerData data = optData.get();
            data.stopFlight(3000 + (1000 * Probability.getRandom(5)));
            data.stopDefrost(15000);
          }
        } else if (!(event instanceof DamageEntityEvent)) {
          return;
        }
        attacker.sendMessage(Text.of(TextColors.YELLOW, "You've hit ", defender.getName(), "!"));
      }
    };
  }

  @Listener
  public void onPlayerCombat(DamageEntityEvent event) {
    Optional<SkyWarsInstance> optInst = manager.getApplicableZone(event.getTargetEntity());

    if (!optInst.isPresent()) {
      return;
    }

    SkyWarsInstance inst = optInst.get();

    createFor(event, inst).parse(event);
  }

  @Listener
  public void onPlayerCombat(CollideEntityEvent.Impact event, @First Projectile projectile) {
    Optional<SkyWarsInstance> optInst = manager.getApplicableZone(projectile);

    if (!optInst.isPresent()) {
      return;
    }

    SkyWarsInstance inst = optInst.get();

    createFor(event, inst).parse(event);
  }
}
