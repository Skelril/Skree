/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.group.jungleraid;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import com.nearce.gamechatter.sponge.GameChatterPlugin;
import com.skelril.nitro.Clause;
import com.skelril.nitro.entity.SafeTeleportHelper;
import com.skelril.nitro.probability.Probability;
import com.skelril.skree.content.zone.LegacyZoneBase;
import com.skelril.skree.service.HighScoreService;
import com.skelril.skree.service.PlayerStateService;
import com.skelril.skree.service.internal.highscore.ScoreTypes;
import com.skelril.skree.service.internal.playerstate.InventoryStorageStateException;
import com.skelril.skree.service.internal.zone.PlayerClassifier;
import com.skelril.skree.service.internal.zone.Zone;
import com.skelril.skree.service.internal.zone.ZoneRegion;
import com.skelril.skree.service.internal.zone.ZoneStatus;
import org.apache.commons.lang3.text.WordUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.EnumTraits;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.skelril.nitro.item.ItemStackFactory.newItemStack;
import static com.skelril.nitro.transformer.ForgeTransformer.tf;
import static com.skelril.skree.service.internal.zone.PlayerClassifier.PARTICIPANT;
import static com.skelril.skree.service.internal.zone.PlayerClassifier.SPECTATOR;

public class JungleRaidInstance extends LegacyZoneBase implements Zone, Runnable {

  private List<Player> participants = new ArrayList<>();

  private Map<Player, Set<Player>> teamMapping = new HashMap<>();
  private Set<Player> freeForAllPlayers = new HashSet<>();
  private Set<Player> blueTeamPlayers = new HashSet<>();
  private Set<Player> redTeamPlayers = new HashSet<>();
  private Map<Player, JungleRaidClass> classMap = new HashMap<>();
  private Map<Player, Player> lastAttackerMap = new HashMap<>();

  private JungleRaidState state = JungleRaidState.LOBBY;
  private long startTime;

  private Location<World> lobbySpawnLocation;
  private Location<World> leftFlagActivationSign;
  private Location<World> rightFlagActivationSign;
  private List<Location<World>> scrollingFlagSigns = new ArrayList<>();

  private Location<World> leftClassActivationSign;
  private Location<World> rightClassActivationSign;
  private List<Location<World>> scrollingClassSigns = new ArrayList<>();

  private int signScrollFlagStart;
  private int signScrollClassStart;

  private FlagEffectData flagData = new FlagEffectData();
  private boolean[] flagState = new boolean[JungleRaidFlag.values().length];

  public JungleRaidInstance(ZoneRegion region) {
    super(region);
  }

  @Override
  public boolean init() {
    setUp();
    remove();
    return true;
  }

  private void setUp() {
    Vector3i offset = getRegion().getMinimumPoint();

    lobbySpawnLocation = new Location<>(getRegion().getExtent(), offset.add(216, 2, 29));
    leftFlagActivationSign = new Location<>(getRegion().getExtent(), offset.add(209, 3, 29));
    rightFlagActivationSign = new Location<>(getRegion().getExtent(), offset.add(209, 3, 23));

    for (int z = 28; z > 23; --z) { // Do this in rerverse so left/right buttons are correct
      scrollingFlagSigns.add(new Location<>(getRegion().getExtent(), offset.add(209, 3, z)));
    }

    for (JungleRaidFlag flag : JungleRaidFlag.values()) {
      flagState[flag.index] = flag.enabledByDefault;
    }

    flagSignPopulate();

    leftClassActivationSign = new Location<>(getRegion().getExtent(), offset.add(209, 3, 22));
    rightClassActivationSign = new Location<>(getRegion().getExtent(), offset.add(209, 3, 18));

    for (int z = 21; z > 18; --z) { // Do this in rerverse so left/right buttons are correct
      scrollingClassSigns.add(new Location<>(getRegion().getExtent(), offset.add(209, 3, z)));
    }

    classSignPopulate();
  }

  private void updateFlagSign(int index) {
    String title = JungleRaidFlag.values()[signScrollFlagStart + index].toString();
    if (title.length() > 15) {
      title = title.substring(0, 15);
    }
    title = WordUtils.capitalizeFully(title.replace("_", " "));

    scrollingFlagSigns.get(index).getTileEntity().get().offer(Keys.SIGN_LINES, Lists.newArrayList(
        Text.EMPTY,
        Text.of(title),
        Text.of(flagState[signScrollFlagStart + index] ? Text.of(TextColors.DARK_GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled")),
        Text.EMPTY
    ));
  }

  private void flagSignPopulate() {
    for (int i = 0; i < scrollingFlagSigns.size(); ++i) {
      updateFlagSign(i);
    }

    boolean isLeftScrollable = signScrollFlagStart == 0;
    leftFlagActivationSign.getTileEntity().get().offer(Keys.SIGN_LINES, Lists.newArrayList(
        Text.EMPTY,
        Text.of(isLeftScrollable ? "" : TextColors.BLUE, "<<"),
        Text.EMPTY,
        Text.EMPTY
    ));
    boolean isRightScrollable = signScrollFlagStart + scrollingFlagSigns.size() == JungleRaidFlag.values().length;
    rightFlagActivationSign.getTileEntity().get().offer(Keys.SIGN_LINES, Lists.newArrayList(
        Text.EMPTY,
        Text.of(isRightScrollable ? "" : TextColors.BLUE, ">>"),
        Text.EMPTY,
        Text.EMPTY
    ));
  }

  public Location<World> getLeftFlagActivationSign() {
    return leftFlagActivationSign;
  }

  public Location<World> getRightFlagActivationSign() {
    return rightFlagActivationSign;
  }

  public void leftFlagListSign() {
    signScrollFlagStart = Math.max(0, signScrollFlagStart - scrollingFlagSigns.size());
    flagSignPopulate();
  }

  public void rightFlagListSign() {
    signScrollFlagStart = Math.min(JungleRaidFlag.values().length - scrollingFlagSigns.size(), signScrollFlagStart + scrollingFlagSigns.size());
    flagSignPopulate();
  }

  public void tryToggleFlagSignAt(Location<World> loc) {
    for (int i = 0; i < scrollingFlagSigns.size(); ++i) {
      if (loc.equals(scrollingFlagSigns.get(i))) {
        flagState[signScrollFlagStart + i] = !flagState[signScrollFlagStart + i];
        updateFlagSign(i);
        break;
      }
    }
  }

  private void updateClassSign(int index) {
    String title = JungleRaidClass.values()[signScrollClassStart + index].toString();
    if (title.length() > 15) {
      title = title.substring(0, 15);
    }
    title = WordUtils.capitalizeFully(title.replace("_", " "));

    scrollingClassSigns.get(index).getTileEntity().get().offer(Keys.SIGN_LINES, Lists.newArrayList(
        Text.EMPTY,
        Text.of(title),
        Text.EMPTY,
        Text.EMPTY
    ));
  }

  private void classSignPopulate() {
    for (int i = 0; i < scrollingClassSigns.size(); ++i) {
      updateClassSign(i);
    }

    boolean isLeftScrollable = signScrollClassStart == 0;
    leftClassActivationSign.getTileEntity().get().offer(Keys.SIGN_LINES, Lists.newArrayList(
        Text.EMPTY,
        Text.of(isLeftScrollable ? "" : TextColors.BLUE, "<<"),
        Text.EMPTY,
        Text.EMPTY
    ));
    boolean isRightScrollable = signScrollClassStart + scrollingClassSigns.size() == JungleRaidClass.values().length;
    rightClassActivationSign.getTileEntity().get().offer(Keys.SIGN_LINES, Lists.newArrayList(
        Text.EMPTY,
        Text.of(isRightScrollable ? "" : TextColors.BLUE, ">>"),
        Text.EMPTY,
        Text.EMPTY
    ));
  }


  public Location<World> getLeftClassActivationSign() {
    return leftClassActivationSign;
  }

  public Location<World> getRightClassActivationSign() {
    return rightClassActivationSign;
  }

  public void leftClassListSign() {
    signScrollClassStart = Math.max(0, signScrollClassStart - scrollingClassSigns.size());
    classSignPopulate();
  }

  public void rightClassListSign() {
    signScrollClassStart = Math.min(JungleRaidClass.values().length - scrollingClassSigns.size(), signScrollClassStart + scrollingClassSigns.size());
    classSignPopulate();
  }

  public void tryUseClassSignAt(Location<World> loc, Player player) {
    for (int i = 0; i < scrollingClassSigns.size(); ++i) {
      if (loc.equals(scrollingClassSigns.get(i))) {
        JungleRaidClass targetClass = JungleRaidClass.values()[signScrollClassStart + i];
        giveBaseEquipment(player, targetClass);
        classMap.put(player, targetClass);
        break;
      }
    }
  }

  public void setFlag(JungleRaidFlag flag, boolean enabled) {
    flagState[flag.index] = enabled;
  }

  public boolean isSuddenDeath() {
    return !isFlagEnabled(JungleRaidFlag.NO_TIME_LIMIT) && System.currentTimeMillis() - getStartTime() >= TimeUnit.MINUTES.toMillis(15);
  }

  public boolean isFlagEnabled(JungleRaidFlag flag) {
    return flagState[flag.index];
  }

  @Override
  public void forceEnd() {
    if (state == JungleRaidState.DONE) {
      Optional<HighScoreService> optHighScores = Sponge.getServiceManager().provide(HighScoreService.class);
      for (Player player : getPlayers(PARTICIPANT)) {
        remove(player);
        optHighScores.ifPresent(highScoreService -> highScoreService.update(player, ScoreTypes.JUNGLE_RAID_WINS, 1));
      }
    }
    remove();
  }

  @Override
  public void run() {
    if (isEmpty() && state != JungleRaidState.IN_PROGRESS) {
      expire();
      return;
    }

    if (state == JungleRaidState.LOBBY) {
      smartStart();
      return;
    }

    if (state == JungleRaidState.INITIALIZE) {
      tryBeginCombat();
      return;
    }

    outOfBoundsCheck();

    Optional<Clause<String, WinType>> optWinner = getWinner();
    if (optWinner.isPresent()) {
      processWin(optWinner.get());
      expire();
      return;
    }
    JungleRaidEffectProcessor.run(this);
  }

  private void outOfBoundsCheck() {
    for (Player player : getPlayers(PARTICIPANT)) {
      if (contains(player)) {
        continue;
      }

      if (player.getLocation().add(0, -1, 0).getBlockType() == BlockTypes.AIR) {
        continue;
      }

      remove(player);
    }
  }

  public JungleRaidState getState() {
    return state;
  }

  public long getStartTime() {
    return startTime;
  }

  public FlagEffectData getFlagData() {
    return flagData;
  }

  private void tryBeginCombat() {
    if (System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(1)) {
      state = JungleRaidState.IN_PROGRESS;
      getPlayerMessageChannel(SPECTATOR).send(Text.of(TextColors.DARK_RED, "LET THE SLAUGHTER BEGIN!"));
    }
  }

  public Optional<Clause<String, WinType>> getWinner() {
    return getWinner(freeForAllPlayers, blueTeamPlayers, redTeamPlayers);
  }

  private Optional<Clause<String, WinType>> getWinner(Collection<Player> ffa, Collection<Player> blue, Collection<Player> red) {
    if (ffa.size() == 1 && blue.isEmpty() && red.isEmpty()) {
      return Optional.of(new Clause<>(ffa.iterator().next().getName(), WinType.SOLO));
    } else if (ffa.isEmpty() && !blue.isEmpty() && red.isEmpty()) {
      return Optional.of(new Clause<>("Blue", WinType.TEAM));
    } else if (ffa.isEmpty() && blue.isEmpty() && !red.isEmpty()) {
      return Optional.of(new Clause<>("Red", WinType.TEAM));
    } else if (ffa.isEmpty() && blue.isEmpty() && red.isEmpty()) {
      return Optional.of(new Clause<>(null, WinType.DRAW));
    }

    return Optional.empty();
  }

  private void processWin(Clause<String, WinType> winClause) {
    state = JungleRaidState.DONE;

    String rawWinMessage;
    switch (winClause.getValue()) {
      case SOLO:
        rawWinMessage = winClause.getKey() + " has won the jungle raid!";
        break;
      case TEAM:
        rawWinMessage = winClause.getKey() + " team has won the jungle raid!";
        break;
      case DRAW:
        rawWinMessage = "The jungle raid was a draw!";
        break;
      default:
        return;
    }

    MessageChannel.TO_ALL.send(Text.of(TextColors.GOLD, rawWinMessage));
    GameChatterPlugin.inst().sendSystemMessage(rawWinMessage);
  }

  @Override
  public Collection<Player> getPlayers(PlayerClassifier classifier) {
    if (classifier == PARTICIPANT) {
      return Lists.newArrayList(participants);
    }
    return super.getPlayers(classifier);
  }


  @Override
  public Clause<Player, ZoneStatus> add(Player player) {
    if (state != JungleRaidState.LOBBY) {
      return new Clause<>(player, ZoneStatus.NO_REJOIN);
    }

    player.setLocation(lobbySpawnLocation);
    Optional<PlayerStateService> optService = Sponge.getServiceManager().provide(PlayerStateService.class);
    if (optService.isPresent()) {
      PlayerStateService service = optService.get();
      try {
        service.storeInventory(player);
        service.releaseInventory(player);

        giveBaseEquipment(player, JungleRaidClass.BALANCED);
      } catch (InventoryStorageStateException e) {
        e.printStackTrace();
        return new Clause<>(player, ZoneStatus.ERROR);
      }
    }

    participants.add(player);

    return new Clause<>(player, ZoneStatus.ADDED);
  }

  private void giveBaseEquipment(Player player, JungleRaidClass jrClass) {
    player.getInventory().clear();

    List<ItemStack> gear = new ArrayList<>();
    switch (jrClass) {
      case MELEE: {
        ItemStack enchantedSword = newItemStack(ItemTypes.IRON_SWORD);
        enchantedSword.offer(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(
            Enchantment.of(EnchantmentTypes.FIRE_ASPECT, 2),
            Enchantment.of(EnchantmentTypes.KNOCKBACK, 2)
        ));

        gear.add(enchantedSword);

        ItemStack shield = newItemStack(ItemTypes.SHIELD);
        gear.add(shield);
        break;
      }
      case LUMBERJACK: {
        ItemStack enchantedAxe = newItemStack(ItemTypes.DIAMOND_AXE);
        enchantedAxe.offer(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(
            Enchantment.of(EnchantmentTypes.SHARPNESS, 3),
            Enchantment.of(EnchantmentTypes.KNOCKBACK, 2)
        ));

        gear.add(enchantedAxe);
        break;
      }
      case ARCHER: {
        ItemStack dmgBow = newItemStack(ItemTypes.BOW);
        dmgBow.offer(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(
            Enchantment.of(EnchantmentTypes.PUNCH, 2)
        ));

        gear.add(dmgBow);

        ItemStack fireBow = newItemStack(ItemTypes.BOW);
        fireBow.offer(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(
            Enchantment.of(EnchantmentTypes.FLAME, 2)
        ));

        gear.add(fireBow);
        break;
      }
      case SNIPER: {
        ItemStack superBow = newItemStack(ItemTypes.BOW);
        superBow.offer(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(
            Enchantment.of(EnchantmentTypes.POWER, 5),
            Enchantment.of(EnchantmentTypes.FLAME, 1)
        ));

        superBow.offer(Keys.ITEM_DURABILITY, jrClass.getArrowAmount());

        gear.add(superBow);

        ItemStack woodSword = newItemStack(ItemTypes.WOODEN_SWORD);
        gear.add(woodSword);
        break;
      }
      case ENGINEER: {
        ItemStack ironSword = newItemStack(ItemTypes.IRON_SWORD);
        gear.add(ironSword);

        ItemStack diamondPickaxe = newItemStack(ItemTypes.DIAMOND_PICKAXE);
        gear.add(diamondPickaxe);

        ItemStack shield = newItemStack(ItemTypes.SHIELD);
        gear.add(shield);
        break;
      }
      case BALANCED: {
        ItemStack standardSword = newItemStack(ItemTypes.IRON_SWORD);
        gear.add(standardSword);

        ItemStack standardBow = newItemStack(ItemTypes.BOW);
        gear.add(standardBow);
        break;
      }
    }

    int tntAmt = jrClass.getTNTAmount();
    int tntStacks = tntAmt / 64;
    int tntRemainder = tntAmt % 64;
    for (int i = 0; i < tntStacks; ++i) {
      gear.add(newItemStack(BlockTypes.TNT, 64));
    }
    if (tntRemainder > 0) {
      gear.add(newItemStack(BlockTypes.TNT, tntRemainder));
    }

    if (jrClass.hasFlintAndSteel()) {
      gear.add(newItemStack(ItemTypes.FLINT_AND_STEEL));
    }
    if (jrClass.hasShears()) {
      gear.add(newItemStack(ItemTypes.SHEARS));
    }
    if (jrClass.hasAxe()) {
      gear.add(newItemStack(ItemTypes.IRON_AXE));
    }
    gear.add(newItemStack(ItemTypes.COOKED_BEEF, 64));
    gear.add(newItemStack(ItemTypes.COMPASS));

    int arrowAmt = jrClass.getArrowAmount();
    int arrowStacks = arrowAmt / 64;
    int arrowRemainder = arrowAmt % 64;
    for (int i = 0; i < arrowStacks; ++i) {
      gear.add(newItemStack(ItemTypes.ARROW, 64));
    }
    if (arrowRemainder > 0) {
      gear.add(newItemStack(ItemTypes.ARROW, arrowRemainder));
    }

    for (ItemStack stack : gear) {
      player.getInventory().offer(stack);
    }

    tf(player).inventoryContainer.detectAndSendChanges();
  }

  private void giveTeamEquipment(Player player, Color teamColor) {
    ItemStack teamHood = newItemStack(ItemTypes.LEATHER_HELMET);
    teamHood.offer(Keys.DISPLAY_NAME, Text.of(TextColors.WHITE, "Team Hood"));
    teamHood.offer(Keys.COLOR, teamColor);
    player.setHelmet(teamHood);

    ItemStack teamChestplate = newItemStack(ItemTypes.LEATHER_CHESTPLATE);
    teamChestplate.offer(Keys.DISPLAY_NAME, Text.of(TextColors.WHITE, "Team Chestplate"));
    teamChestplate.offer(Keys.COLOR, teamColor);
    player.setChestplate(teamChestplate);

    ItemStack teamLeggings = newItemStack(ItemTypes.LEATHER_LEGGINGS);
    teamLeggings.offer(Keys.DISPLAY_NAME, Text.of(TextColors.WHITE, "Team Leggings"));
    teamLeggings.offer(Keys.COLOR, teamColor);
    player.setLeggings(teamLeggings);

    ItemStack teamBoots = newItemStack(ItemTypes.LEATHER_BOOTS);
    teamBoots.offer(Keys.DISPLAY_NAME, Text.of(TextColors.WHITE, "Team Boots"));
    teamBoots.offer(Keys.COLOR, teamColor);
    player.setBoots(teamBoots);
  }

  private void resetPlayerProperties(Player player) {
    player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).orElse(20D));
    player.offer(Keys.FOOD_LEVEL, 20);
    player.offer(Keys.SATURATION, 20D);
    player.offer(Keys.EXHAUSTION, 0D);

    player.offer(Keys.POTION_EFFECTS, new ArrayList<>());
  }

  private void addPlayer(Player player, Supplier<Location<World>> startingPos, Color teamColor, JungleRaidClass jrClass) {
    giveBaseEquipment(player, jrClass);
    giveTeamEquipment(player, teamColor);

    resetPlayerProperties(player);

    player.setLocation(startingPos.get());
  }

  public void addFFAPlayer(Player player, JungleRaidClass jrClass) {
    addPlayer(player, this::getRandomLocation, Color.WHITE, jrClass);
    freeForAllPlayers.add(player);
    teamMapping.put(player, freeForAllPlayers);
  }

  public void addBluePlayer(Player player, JungleRaidClass jrClass) {
    Location<World> spawnPoint = getRandomLocation();
    addPlayer(player, () -> spawnPoint, Color.BLUE, jrClass);
    blueTeamPlayers.add(player);
    teamMapping.put(player, blueTeamPlayers);
  }

  public void addRedPlayer(Player player, JungleRaidClass jrClass) {
    Location<World> spawnPoint = getRandomLocation();
    addPlayer(player, () -> spawnPoint, Color.RED, jrClass);
    redTeamPlayers.add(player);
    teamMapping.put(player, redTeamPlayers);
  }

  public void smartStart() {
    List<Player> ffaList = new ArrayList<>();
    List<Player> redList = new ArrayList<>();
    List<Player> blueList = new ArrayList<>();

    Collection<Player> containedPlayers = getPlayers(PARTICIPANT);
    if (containedPlayers.size() <= 1) {
      return;
    }

    for (Player player : containedPlayers) {
      BlockState state = player.getLocation().add(0, -1, 0).getBlock();
      if (state.getType() != BlockTypes.WOOL) {
        return;
      }

      Optional<?> optColor = state.getTraitValue(EnumTraits.WOOL_COLOR);
      if (optColor.isPresent()) {
        DyeColor color = (DyeColor) optColor.get();
        if (color == DyeColors.RED) {
          redList.add(player);
        } else if (color == DyeColors.BLUE) {
          blueList.add(player);
        } else if (color == DyeColors.WHITE) {
          ffaList.add(player);
        } else {
          return;
        }
      }
    }

    if (getWinner(ffaList, blueList, redList).isPresent()) {
      getPlayerMessageChannel(SPECTATOR).send(Text.of(TextColors.RED, "All players are on one team, the game will not start."));
      return;
    }

    ffaList.stream().forEach(p -> addFFAPlayer(p, classMap.getOrDefault(p, JungleRaidClass.BALANCED)));
    redList.stream().forEach(p -> addRedPlayer(p, classMap.getOrDefault(p, JungleRaidClass.BALANCED)));
    blueList.stream().forEach(p -> addBluePlayer(p, classMap.getOrDefault(p, JungleRaidClass.BALANCED)));

    state = JungleRaidState.INITIALIZE;
    startTime = System.currentTimeMillis();
  }

  public Location<World> getRandomLocation() {
    Vector3i offset = getRegion().getMinimumPoint();
    Vector3i boundingBox = getRegion().getBoundingBox();

    while (true) {
      Vector3i randomDest = new Vector3i(
          Probability.getRandom(boundingBox.getX()),
          Probability.getRangedRandom(16, 80),
          Probability.getRandom(boundingBox.getZ())
      ).add(offset);

      Optional<Location<World>> optSafeDest = SafeTeleportHelper.getSafeDest(
          new Location<>(getRegion().getExtent(), randomDest)
      );

      if (optSafeDest.isPresent()) {
        Location<World> safeDest = optSafeDest.get();
        if (16 < safeDest.getY() && safeDest.getY() < 79) {
          return safeDest.add(.5, 0, .5);
        }
      }
    }
  }

  @Override
  public Clause<Player, ZoneStatus> remove(Player player) {
    resetPlayerProperties(player);
    playerLost(player);
    tryInventoryRestore(player);

    return super.remove(player);
  }

  public void tryInventoryRestore(Player player) {
    Optional<PlayerStateService> optService = Sponge.getServiceManager().provide(PlayerStateService.class);
    if (optService.isPresent()) {
      PlayerStateService service = optService.get();
      service.loadInventoryIfStored(player);
    }
  }

  public void playerLost(Player player) {
    participants.remove(player);

    Set<Player> teamPlayers = teamMapping.remove(player);
    if (teamPlayers != null) {
      teamPlayers.remove(player);

      player.getInventory().clear();
    }
  }

  public Optional<Color> getTeamColor(Player player) {
    Set<Player> playerTeam = teamMapping.get(player);
    if (playerTeam == redTeamPlayers) {
      return Optional.of(Color.RED);
    } else if (playerTeam == blueTeamPlayers) {
      return Optional.of(Color.BLUE);
    } else if (playerTeam == freeForAllPlayers) {
      return Optional.of(Color.WHITE);
    }
    return Optional.empty();
  }

  public Optional<JungleRaidClass> getClass(Player player) {
    return Optional.ofNullable(classMap.get(player));
  }

  private void payPlayer(Player player) {

  }

  public void recordAttack(Player attacker, Player defender) {
    lastAttackerMap.put(defender, attacker);
  }

  public Optional<Player> getLastAttacker(Player defender) {
    return Optional.ofNullable(lastAttackerMap.get(defender));
  }

  public boolean isFriendlyFire(Player attacker, Player defender) {
    Set<Player> attackerTeam = teamMapping.get(attacker);
    Set<Player> defenderTeam = teamMapping.get(defender);

    /* We want identity comparison to prevent expensive list comparisons */
    return attackerTeam == defenderTeam && attackerTeam != freeForAllPlayers && attackerTeam != null;
  }
}
