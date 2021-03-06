/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.group.jungleraid;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import com.skelril.nitro.item.ItemDropper;
import com.skelril.nitro.item.ItemStackFactory;
import com.skelril.nitro.probability.Probability;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.service.internal.zone.PlayerClassifier;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Firework;
import org.spongepowered.api.entity.projectile.ThrownPotion;
import org.spongepowered.api.item.FireworkEffect;
import org.spongepowered.api.item.FireworkShapes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static com.skelril.nitro.item.ItemStackFactory.newItemStack;

public class JungleRaidEffectProcessor {
  private static final Random RANDOM = new Random();

  public static void run(JungleRaidInstance inst) {
    globalPotionEffects(inst);
    titanMode(inst);
    distributor(inst);
    randomRockets(inst);
  }

  private static void globalPotionEffects(JungleRaidInstance inst) {
    boolean isSuddenDeath = inst.isSuddenDeath();

    for (Player player : inst.getPlayers(PlayerClassifier.PARTICIPANT)) {
      if (isSuddenDeath) {
        List<PotionEffect> potionEffects = player.getOrElse(Keys.POTION_EFFECTS, new ArrayList<>());
        potionEffects.add(PotionEffect.of(PotionEffectTypes.GLOWING, 1, 20 * 20));
        player.offer(Keys.POTION_EFFECTS, potionEffects);
      }
    }
  }

  private static void titanMode(JungleRaidInstance inst) {
    FlagEffectData data = inst.getFlagData();
    Collection<Player> players = inst.getPlayers(PlayerClassifier.PARTICIPANT);

    if (inst.isFlagEnabled(JungleRaidFlag.TITAN_MODE) && data.titan == null) {
      Player player = Probability.pickOneOf(players);
      data.titan = player.getUniqueId();

      ItemStack teamHood = newItemStack(ItemTypes.LEATHER_HELMET);
      teamHood.offer(Keys.DISPLAY_NAME, Text.of(TextColors.WHITE, "Titan Hood"));
      teamHood.offer(Keys.COLOR, Color.BLACK);

      player.setHelmet(teamHood);
    }

    for (Player player : players) {
      if (inst.isFlagEnabled(JungleRaidFlag.TITAN_MODE) && player.getUniqueId().equals(data.titan)) {
        List<PotionEffect> potionEffects = player.getOrElse(Keys.POTION_EFFECTS, new ArrayList<>());
        potionEffects.add(PotionEffect.of(PotionEffectTypes.NIGHT_VISION, 1, 20 * 20));
        player.offer(Keys.POTION_EFFECTS, potionEffects);
      }
    }
  }

  private static ItemStackSnapshot createPotionItemSnapshot() {
    PotionEffectType type = Probability.pickOneOf(Sponge.getRegistry().getAllOf(PotionEffectType.class));

    ItemStack potionItem = ItemStackFactory.newItemStack(ItemTypes.SPLASH_POTION);
    potionItem.offer(Keys.POTION_EFFECTS, Lists.newArrayList(PotionEffect.of(type, 1, type.isInstant() ? 1 : 20 * 10)));

    return potionItem.createSnapshot();
  }

  private static void distributor(JungleRaidInstance inst) {
    FlagEffectData data = inst.getFlagData();
    boolean isSuddenDeath = inst.isSuddenDeath();
    if (isSuddenDeath) {
      data.amt = 100;
    }

    if (inst.isFlagEnabled(JungleRaidFlag.END_OF_DAYS) || inst.isFlagEnabled(JungleRaidFlag.GRENADES) || inst.isFlagEnabled(JungleRaidFlag.POTION_PLUMMET) || isSuddenDeath) {

      Vector3i bvMax = inst.getRegion().getMaximumPoint();
      Vector3i bvMin = inst.getRegion().getMinimumPoint();

      for (int i = 0; i < Probability.getRangedRandom(data.amt / 3, data.amt); i++) {

        Location<World> testLoc = new Location<>(
            inst.getRegion().getExtent(),
            Probability.getRangedRandom(bvMin.getX(), bvMax.getX()),
            bvMax.getY(),
            Probability.getRangedRandom(bvMin.getZ(), bvMax.getZ())
        );

        if (testLoc.getBlockType() != BlockTypes.AIR) {
          continue;
        }

        if (inst.isFlagEnabled(JungleRaidFlag.END_OF_DAYS) || isSuddenDeath) {
          PrimedTNT explosive = (PrimedTNT) inst.getRegion().getExtent().createEntity(EntityTypes.PRIMED_TNT, testLoc.getPosition());
          explosive.setVelocity(new Vector3d(
              RANDOM.nextDouble() * 2.0 - 1,
              RANDOM.nextDouble() * 2 * -1,
              RANDOM.nextDouble() * 2.0 - 1
          ));
          explosive.offer(Keys.FUSE_DURATION, 20 * 4);

          // TODO used to have a 1/4 chance of creating fire
          inst.getRegion().getExtent().spawnEntity(explosive);
        }

        if (inst.isFlagEnabled(JungleRaidFlag.POTION_PLUMMET)) {
          ItemStackSnapshot potionItemStack = createPotionItemSnapshot();

          for (int ii = Probability.getRandom(5); ii > 0; --ii) {
            ThrownPotion potion = (ThrownPotion) inst.getRegion().getExtent().createEntity(EntityTypes.SPLASH_POTION, testLoc.getPosition());
            potion.setVelocity(new Vector3d(
                RANDOM.nextDouble() * 2.0 - 1,
                0,
                RANDOM.nextDouble() * 2.0 - 1
            ));

            potion.offer(Keys.REPRESENTED_ITEM, potionItemStack);
            inst.getRegion().getExtent().spawnEntity(potion);
          }
        }

        if (inst.isFlagEnabled(JungleRaidFlag.GRENADES)) {
          new ItemDropper(testLoc).dropStacks(
              Lists.newArrayList(newItemStack(ItemTypes.SNOWBALL, Probability.getRandom(3)))
          );
        }
      }
      if (data.amt < 150 && Probability.getChance(inst.isFlagEnabled(JungleRaidFlag.SUPER) ? 9 : 25)) {
        ++data.amt;
      }
    }
  }

  private static void randomRockets(JungleRaidInstance inst) {
    if (inst.isFlagEnabled(JungleRaidFlag.RANDOM_ROCKETS)) {
      for (final Player player : inst.getPlayers(PlayerClassifier.PARTICIPANT)) {
        if (!Probability.getChance(30)) {
          continue;
        }
        for (int i = 0; i < 5; i++) {
          Task.builder().delayTicks(i * 4).execute(() -> {
            Location targetLocation = player.getLocation();
            Firework firework = (Firework) inst.getRegion().getExtent().createEntity(EntityTypes.FIREWORK, targetLocation.getPosition());
            FireworkEffect fireworkEffect = FireworkEffect.builder()
                .flicker(Probability.getChance(2))
                .trail(Probability.getChance(2))
                .color(Color.RED)
                .fade(Color.YELLOW)
                .shape(FireworkShapes.BURST)
                .build();
            firework.offer(Keys.FIREWORK_EFFECTS, Lists.newArrayList(fireworkEffect));
            firework.offer(Keys.FIREWORK_FLIGHT_MODIFIER, Probability.getRangedRandom(2, 5));
            inst.getRegion().getExtent().spawnEntity(firework);
          }).submit(SkreePlugin.inst());
        }
      }
    }
  }
}
