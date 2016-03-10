/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.group.goldrush;

import com.flowpowered.math.vector.Vector3i;
import com.skelril.nitro.Clause;
import com.skelril.nitro.probability.Probability;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.content.market.MarketImplUtil;
import com.skelril.skree.content.modifier.Modifiers;
import com.skelril.skree.content.registry.item.CustomItemTypes;
import com.skelril.skree.content.registry.item.generic.PrizeBox;
import com.skelril.skree.content.zone.LegacyZoneBase;
import com.skelril.skree.service.MarketService;
import com.skelril.skree.service.ModifierService;
import com.skelril.skree.service.WorldService;
import com.skelril.skree.service.internal.zone.Zone;
import com.skelril.skree.service.internal.zone.ZoneBoundingBox;
import com.skelril.skree.service.internal.zone.ZoneRegion;
import com.skelril.skree.service.internal.zone.ZoneStatus;
import net.minecraft.inventory.IInventory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.trait.BooleanTraits;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.skelril.nitro.item.ItemStackFactory.newItemStack;
import static com.skelril.nitro.transformer.ForgeTransformer.tf;
import static com.skelril.skree.content.market.MarketImplUtil.format;
import static com.skelril.skree.content.registry.item.generic.PrizeBox.makePrizeBox;

public class GoldRushInstance extends LegacyZoneBase implements Zone, Runnable {
    // Constants
    private static final BigDecimal MIN_START_RISK = new BigDecimal(100);
    private static final BigDecimal GRAB_RATIO = new BigDecimal(.2);
    private static final BigDecimal PIVOTAL_VALUE = new BigDecimal(5500);

    private ZoneBoundingBox startingRoom, keyRoom, flashMemoryRoom;

    private ZoneBoundingBox flashMemoryDoor, rewardRoomDoor;

    // Block - Should be flipped
    private ConcurrentHashMap<Location<World>, Boolean> leverBlocks = new ConcurrentHashMap<>();
    private List<Location<World>> floodBlocks = new ArrayList<>();
    private List<Location<World>> chestBlocks = new ArrayList<>();

    // Block - Is unlocked
    private List<Location<World>> locks = new ArrayList<>();
    private Location rewardChest;

    // Session
    private long startTime = -1;
    private BigDecimal multiplier = BigDecimal.ONE;
    private BigDecimal lootSplit = BigDecimal.ZERO;
    private int playerMod = 0;
    private BlockType floodBlockType = BlockTypes.WATER;
    protected Map<UUID, BigDecimal> cofferRisk = new HashMap<>();
    private boolean keysTriggered = false;
    private boolean checkingKeys = true;
    private boolean leversTriggered = false;
    private boolean checkingLevers = true;
    private long lastLeverSwitch = System.currentTimeMillis();
    private long lastFlood = System.currentTimeMillis();

    public GoldRushInstance(ZoneRegion region) {
        super(region);
        setup();
        remove();
    }

    private void setup() {
        Vector3i offset = getRegion().getMinimumPoint();

        rewardChest = new Location<>(
                getRegion().getExtent(),
                offset.getX() + 15,
                offset.getY() + 2,
                offset.getZ() + 6
        );

        startingRoom = new ZoneBoundingBox(offset.add(2, 1, 76), new Vector3i(27, 7, 14));
        keyRoom = new ZoneBoundingBox(offset.add(1, 1, 36), new Vector3i(30, 7, 39));
        flashMemoryRoom = new ZoneBoundingBox(offset.add(11, 1, 17), new Vector3i(9, 7, 19));

        flashMemoryDoor = new ZoneBoundingBox(offset.add(14, 1, 36), new Vector3i(3, 3, 1));
        rewardRoomDoor = new ZoneBoundingBox(offset.add(14, 1, 16), new Vector3i(3, 3, 1));

        findChestAndKeys();         // Setup room one
        findLeversAndFloodBlocks(); // Setup room two
    }

    @Override
    public boolean init() {
        resetChestAndKeys();
        resetLevers();
        resetFloodType();
        drainAll();
        setDoor(flashMemoryDoor, BlockTypes.IRON_BLOCK);
        setDoor(rewardRoomDoor, BlockTypes.IRON_BLOCK);
        return true;
    }

    public void tryToStart() {
        BigDecimal coffersNeeded = getCoffersNeeded();
        if (coffersNeeded.compareTo(BigDecimal.ZERO) >= 0) {
            MessageChannel channel = MessageChannel.fixed(getContained(Player.class));
            channel.send(Text.of(TextColors.RED, "Your party doesn't have a high enough coffer risk!"));
            channel.send(Text.of(TextColors.RED, "At least ", coffersNeeded, " more coffers must be risked."));
            return;
        }
        readyPlayers();
        calculateLootSplit();
        startTime = System.currentTimeMillis(); // Reset tryToStart clock
        populateChest();                        // Add content
    }

    public boolean isLocked() {
        return startTime != -1;
    }

    public boolean isComplete() {
        return checkKeys() && checkLevers();
    }

    private void findChestAndKeys() {
        keyRoom.forAll((pt) -> {
            BlockState block = getRegion().getExtent().getBlock(pt);
            if (block.getType() == BlockTypes.CHEST) {
                // TODO Sponge port
                Optional<TileEntity> optTileEnt = getRegion().getExtent().getTileEntity(pt);
                if (optTileEnt.isPresent() && optTileEnt.get() instanceof IInventory) {
                    ((IInventory) optTileEnt.get()).clear();
                }

                chestBlocks.add(new Location<>(getRegion().getExtent(), pt));
            } else if (block.getType() == BlockTypes.WALL_SIGN) {
                Optional<org.spongepowered.api.block.tileentity.TileEntity> optTileEnt = getRegion().getExtent().getTileEntity(pt);
                if (!optTileEnt.isPresent()) {
                    return;
                }

                Optional<List<Text>> optTexts = optTileEnt.get().get(Keys.SIGN_LINES);
                if (optTexts.isPresent()) {
                    List<Text> text = optTexts.get();
                    text.set(2, Text.of("- Locked -"));
                    text.set(3, Text.of("Unlocked"));
                    optTileEnt.get().offer(Keys.SIGN_LINES, text);
                }

                locks.add(new Location<>(getRegion().getExtent(), pt));
            }
        });
    }

    private void populateChest() {
        for (Location<World> chest : chestBlocks) {
            // TODO Sponge port
            Optional<TileEntity> optTileEnt = chest.getTileEntity();
            if (optTileEnt.isPresent() && optTileEnt.get() instanceof IInventory) {
                IInventory inventory = ((IInventory) optTileEnt.get());
                int iterationTimes = Probability.getRandom(27);
                for (int i = iterationTimes; i > 0; --i) {
                    ItemStack targetStack;

                    int goldRand = Probability.getRandom(Probability.getRandom(Probability.getRandom(64)));

                    if (Probability.getChance(1000)) {
                        targetStack = makePrizeBox(newItemStack(BlockTypes.GOLD_BLOCK, goldRand));
                    } else {
                        targetStack = makePrizeBox(newItemStack(ItemTypes.GOLD_INGOT, goldRand));
                    }

                    inventory.setInventorySlotContents(
                            Probability.getRandom(inventory.getSizeInventory()) - 1,
                            tf(targetStack)
                    );
                }

                if (Probability.getChance(10000 / iterationTimes)) {
                    inventory.setInventorySlotContents(
                            Probability.getRandom(inventory.getSizeInventory()) - 1,
                            tf(makePrizeBox(newItemStack(CustomItemTypes.PHANTOM_HYMN)))
                    );
                }
            }
        }

        for (int i = 0; i < 2; i++) {
            Optional<TileEntity> optTileEnt = Probability.pickOneOf(chestBlocks).getTileEntity();
            if (optTileEnt.isPresent() && optTileEnt.get() instanceof IInventory) {
                IInventory inventory = ((IInventory) optTileEnt.get());
                inventory.setInventorySlotContents(
                        Probability.getRandom(inventory.getSizeInventory()) - 1,
                        new net.minecraft.item.ItemStack(CustomItemTypes.GOLD_RUSH_KEY, 1, i)
                );
            }
        }
    }

    private void resetChestAndKeys() {
        for (Location<World> chest : chestBlocks) {
            // TODO Sponge port
            Optional<TileEntity> optTileEnt = chest.getTileEntity();
            if (optTileEnt.isPresent() && optTileEnt.get() instanceof IInventory) {
                ((IInventory) optTileEnt.get()).clear();
            }
        }

        for (Location<World> lock : locks) {
            Optional<TileEntity> optTileEnt = lock.getTileEntity();
            if (optTileEnt.isPresent()) {
                Optional<List<Text>> optTexts = optTileEnt.get().get(Keys.SIGN_LINES);
                if (optTexts.isPresent()) {
                    List<Text> text = optTexts.get();
                    text.set(2, Text.of("- Locked -"));
                    text.set(3, Text.of("Unlocked"));
                    optTileEnt.get().offer(Keys.SIGN_LINES, text);
                }
            }
        }

        keysTriggered = false;
        checkingKeys = true;
    }

    private void findLeversAndFloodBlocks() {

        Vector3i min = flashMemoryRoom.getMinimumPoint();
        Vector3i max = flashMemoryRoom.getMaximumPoint();

        int minX = min.getX();
        int minZ = min.getZ();
        int minY = min.getY();
        int maxX = max.getX();
        int maxZ = max.getZ();
        int maxY = max.getY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; --y) {
                    BlockState state = getRegion().getExtent().getBlock(x, y, z);
                    if (state.getType() == BlockTypes.LEVER) {
                        Location<World> loc = new Location<>(getRegion().getExtent(), x, y, z);
                        loc.getExtent().setBlock(
                                loc.getBlockPosition(),
                                state.withTrait(BooleanTraits.LEVER_POWERED, false).orElse(state),
                                true,
                                Cause.of(NamedCause.source(SkreePlugin.container()))
                        );

                        leverBlocks.put(loc, !Probability.getChance(3));
                        for (int i = y; i < maxY; i++) {
                            BlockType type = getRegion().getExtent().getBlockType(x, i, z);
                            if (type == BlockTypes.AIR) {
                                floodBlocks.add(new Location<>(getRegion().getExtent(), x, i, z));
                                break;
                            }
                        }
                        break; // One lever a column only
                    }
                }
            }
        }
    }

    public boolean checkLevers() {
        if (!checkingLevers) {
            return leversTriggered;
        }

        for (Map.Entry<Location<World>, Boolean> lever : leverBlocks.entrySet()) {
            boolean leverState = lever.getKey().getBlock().getTraitValue(BooleanTraits.LEVER_POWERED).orElse(false);
            if (leverState != lever.getValue()) return false;
        }
        return true;
    }

    public void unlockLevers() {
        drainAll();
        setDoor(rewardRoomDoor, BlockTypes.AIR);
        leversTriggered = true;
        checkingLevers = false;
    }

    private void resetLevers() {
        leversTriggered = false;
        checkingLevers = true;

        for (Location<World> entry : leverBlocks.keySet()) {
            BlockState state = entry.getBlock();

            entry.getExtent().setBlock(
                    entry.getBlockPosition(),
                    state.withTrait(BooleanTraits.LEVER_POWERED, false).orElse(state),
                    true,
                    Cause.of(NamedCause.source(SkreePlugin.container()))
            );

            leverBlocks.put(entry, !Probability.getChance(3));
        }
    }

    public Location getRewardChestLoc() {
        return rewardChest;
    }

    @Override
    public void forceEnd() {
        resetChestAndKeys();
    }

    @Override
    public Clause<Player, ZoneStatus> add(Player player) {
        if (isLocked()) {
            return new Clause<>(player, ZoneStatus.NO_REJOIN);
        }

        player.setLocation(new Location<>(getRegion().getExtent(), startingRoom.getCenter()));
        return new Clause<>(player, ZoneStatus.ADDED);
    }

    @Override
    public Clause<Player, ZoneStatus> remove(Player player) {
        // TODO remove any items given
        WorldService service = Sponge.getServiceManager().provideUnchecked(WorldService.class);
        player.setLocation(service.getEffectWrapper("Main").getWorlds().iterator().next().getSpawnLocation());
        return new Clause<>(player, ZoneStatus.REMOVED);
    }

    private BigDecimal getCoffersNeeded() {
        Collection<Player> cPlayers = getContained(Player.class);
        Iterator<Player> it = cPlayers.iterator();

        BigDecimal totalRisk = BigDecimal.ZERO;

        while (it.hasNext()) {
            Player next = it.next();
            BigDecimal origCharge = cofferRisk.get(next.getUniqueId());
            if (origCharge == null) {
                continue;
            }
            totalRisk = totalRisk.add(origCharge);
        }

        return MIN_START_RISK.subtract(totalRisk);
    }

    private void calculateLootSplit() {
        multiplier = BigDecimal.ONE;

        Collection<Player> cPlayers = getContained(Player.class);
        for (Player next : cPlayers) {
            BigDecimal origCharge = cofferRisk.get(next.getUniqueId());
            if (origCharge == null) {
                continue;
            }

            lootSplit = lootSplit.add(origCharge.multiply(new BigDecimal(.05)));
        }
        lootSplit = lootSplit.divide(new BigDecimal(cPlayers.size()), RoundingMode.HALF_DOWN);

        playerMod = Math.max(1, cPlayers.size() / 2);
        if (Probability.getChance(35)) multiplier = multiplier.multiply(BigDecimal.TEN);
        if (Probability.getChance(15)) multiplier = multiplier.multiply(new BigDecimal(2));

        Optional<ModifierService> optService = Sponge.getServiceManager().provide(ModifierService.class);
        if (optService.isPresent()) {
            ModifierService service = optService.get();
            if (service.isActive(Modifiers.QUAD_GOLD_RUSH)) multiplier = multiplier.multiply(new BigDecimal(4));
        }
    }

    private void readyPlayers() {
        Collection<Player> players = getContained(Player.class);
        for (Player player : players) {
            // Reset vitals
            player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).orElse(20D));
            player.offer(Keys.FOOD_LEVEL, 20);
            player.offer(Keys.SATURATION, 20D);
            player.offer(Keys.EXHAUSTION, 0D);

            // Move player into the game
            player.setLocation(new Location<>(getRegion().getExtent(), keyRoom.getCenter()));
        }

        // Partner talk
        Task.builder().execute(() -> {
            MessageChannel.fixed(players).send(Text.of(
                    TextColors.YELLOW,
                    "[Partner] I've disabled the security systems for now."
            ));
            Task.builder().execute(() -> {
                MessageChannel.fixed(players).send(Text.of(
                        TextColors.YELLOW,
                        "[Partner] For your sake kid I hope you can move quickly."
                ));
            }).delay(1, TimeUnit.SECONDS).submit(SkreePlugin.inst());
        }).delay(1, TimeUnit.SECONDS).submit(SkreePlugin.inst());
    }

    @Override
    public void run() {
        if (isEmpty()) {
            expire();
            return;
        }

        if (!isLocked()) return; // If it's not locked things haven't been started yet
        if (System.currentTimeMillis() - startTime > TimeUnit.MINUTES.toMillis(7)) {
            expire();
            return;
        }

        if (checkKeys()) {
            unlockKeys();
            if (getContained(Player.class).stream().filter(p -> keyRoom.contains(p.getLocation().getPosition())).count() > 0) {
                setDoor(flashMemoryDoor, BlockTypes.AIR);
            } else {
                setDoor(flashMemoryDoor, BlockTypes.IRON_BLOCK);
                if (checkLevers()) {
                    unlockLevers();
                } else {
                    randomizeLevers();
                    checkFloodType();
                    flood();
                }
            }
        }
    }

    public void refundPlayer(Player player) {
        BigDecimal fee = cofferRisk.get(player.getUniqueId());

        // They didn't pay, CHEATER!!!
        if (fee == null) return;
        MarketImplUtil.setBalanceTo(player, fee.add(MarketImplUtil.getMoney(player)), Cause.of(NamedCause.source(this)));
        remove(player);
        player.sendMessage(Text.of(TextColors.YELLOW, "[Partner] These @$#&!@# restarts... Here, have your bail money..."));
    }

    public boolean payPlayer(Player player) {
        BigDecimal fee = cofferRisk.get(player.getUniqueId());
        // They didn't pay, CHEATER!!!
        if (fee == null) return false;

        net.minecraft.item.ItemStack[] itemStacks = tf(player).inventory.mainInventory;
        BigDecimal goldValue = BigDecimal.ZERO;
        BigDecimal itemValue = BigDecimal.ZERO;

        Optional<MarketService> optService = Sponge.getServiceManager().provide(MarketService.class);

        for (int i = 0; i < itemStacks.length; ++i) {
            net.minecraft.item.ItemStack is = itemStacks[i];

            if (is == null || is.getItem() != CustomItemTypes.PRIZE_BOX) continue;

            Optional<ItemStack> optOpened = PrizeBox.getPrizeStack(is);
            if (optService.isPresent() && optOpened.isPresent()) {
                MarketService service = optService.get();
                ItemStack opened = optOpened.get();
                Optional<BigDecimal> value = service.getPrice(opened);
                if (value.isPresent()) {
                    if (opened.getItem() == ItemTypes.GOLD_NUGGET || opened.getItem() == ItemTypes.GOLD_INGOT || opened.getItem() == BlockTypes.GOLD_BLOCK.getItem().get()) {
                        goldValue = goldValue.add(value.get());
                        itemStacks[i] = null;
                    } else {
                        itemValue = itemValue.add(value.get());
                        itemStacks[i] = tf(opened);
                    }
                }
            }
        }

        BigDecimal totalGrabbed = goldValue.add(itemValue);
        BigDecimal grabRatio = GRAB_RATIO.multiply(totalGrabbed.divide(PIVOTAL_VALUE, 2, RoundingMode.DOWN));
        BigDecimal splitBoost = multiplier.multiply(fee.multiply(grabRatio));
        BigDecimal personalLootSplit = multiplier.multiply(lootSplit).add(splitBoost);

        tf(player).inventoryContainer.detectAndSendChanges();

        player.sendMessage(Text.of(TextColors.YELLOW, "You obtain: "));
        player.sendMessage(Text.of(TextColors.YELLOW, " - Bail: ", format(fee)));
        player.sendMessage(Text.of(TextColors.YELLOW, " - Split: ", format(lootSplit), ", Boost: ", format(grabRatio.multiply(new BigDecimal(100))), "%"));
        if (grabRatio.compareTo(BigDecimal.ZERO) > 0) {
            player.sendMessage(Text.of(TextColors.YELLOW, "   - Boost value: ", format(splitBoost)));
        }
        if (goldValue.compareTo(BigDecimal.ZERO) > 0) {
            player.sendMessage(Text.of(TextColors.YELLOW, " - Gold: ", format(goldValue)));
        }

        BigDecimal total = fee.add(personalLootSplit).add(goldValue);
        player.sendMessage(Text.of(TextColors.YELLOW, "Total: ", format(total)));

        MarketImplUtil.setBalanceTo(player, total.add(MarketImplUtil.getMoney(player)), Cause.of(NamedCause.source(this)));
        remove(player);
        return true;
    }

    public List<Location<World>> getLockLocations() {
        return locks;
    }

    public boolean checkKeys() {
        if (!checkingKeys) {
            return keysTriggered;
        }

        for (Location<World> lock : locks) {
            Optional<org.spongepowered.api.block.tileentity.TileEntity> optTileEnt = lock.getTileEntity();
            if (!optTileEnt.isPresent()) {
                return false;
            }

            org.spongepowered.api.block.tileentity.TileEntity tileEnt = optTileEnt.get();
            Optional<List<Text>> optTexts = tileEnt.get(Keys.SIGN_LINES);

            if (!optTexts.isPresent()) {
                return false;
            }

            if (optTexts.get().get(2).toPlain().startsWith("-")) return false;
        }
        return true;
    }

    public void unlockKeys() {
        keysTriggered = true;
        checkingKeys = false;
    }

    private void setDoor(ZoneBoundingBox door, BlockType type) {
        door.forAll((pt) -> getRegion().getExtent().setBlockType(pt, type, true, Cause.of(NamedCause.source(SkreePlugin.container()))));
    }

    private void drainAll() {
        flashMemoryRoom.forAll((pt) -> {
            BlockType type = getRegion().getExtent().getBlockType(pt);
            if (type == BlockTypes.WATER || type == BlockTypes.FLOWING_WATER || type == BlockTypes.LAVA || type == BlockTypes.FLOWING_LAVA) {
                getRegion().getExtent().setBlockType(pt, BlockTypes.AIR, true, Cause.of(NamedCause.source(SkreePlugin.container())));
            }
        });
    }

    private void randomizeLevers() {

        if (System.currentTimeMillis() - lastLeverSwitch >= TimeUnit.SECONDS.toMillis(14)) {
            for (Location<World> entry : leverBlocks.keySet()) {
                BlockState state = entry.getBlock();
                entry.getExtent().setBlock(
                        entry.getBlockPosition(),
                        state.withTrait(BooleanTraits.LEVER_POWERED, false).orElse(state),
                        true,
                        Cause.of(NamedCause.source(SkreePlugin.container()))
                );
                leverBlocks.put(entry, !Probability.getChance(3));
            }
            lastLeverSwitch = System.currentTimeMillis();
            randomizeLevers();
        } else if (System.currentTimeMillis() - lastLeverSwitch == 0) {
            for (Location<World> entry : leverBlocks.keySet()) {
                Location<World> targLoc = entry.add(0, -2, 0);
                targLoc.getExtent().setBlockType(
                        targLoc.getBlockPosition(),
                        BlockTypes.STONEBRICK,
                        true,
                        Cause.of(NamedCause.source(SkreePlugin.container()))
                );
            }

            Task.builder().execute(() -> {
                for (Map.Entry<Location<World>, Boolean> entry : leverBlocks.entrySet()) {
                    Location<World> targLoc = entry.getKey().add(0, -2, 0);
                    targLoc.getExtent().setBlockType(
                            targLoc.getBlockPosition(),
                            entry.getValue() ? BlockTypes.REDSTONE_BLOCK : BlockTypes.STONEBRICK,
                            true,
                            Cause.of(NamedCause.source(SkreePlugin.container()))
                    );
                }

                Task.builder().execute(this::randomizeLevers).delayTicks(15).submit(SkreePlugin.inst());
            }).delayTicks(15).submit(SkreePlugin.inst());
        } else {
            for (Location<World> entry : leverBlocks.keySet()) {
                Location<World> targLoc = entry.add(0, -2, 0);
                targLoc.getExtent().setBlockType(
                        targLoc.getBlockPosition(),
                        BlockTypes.STONEBRICK,
                        true,
                        Cause.of(NamedCause.source(SkreePlugin.container()))
                );
            }
        }
    }

    private void checkFloodType() {
        if (floodBlockType == BlockTypes.FLOWING_LAVA) {
            return;
        }

        for (Player player : getContained(Player.class)) {
            net.minecraft.item.ItemStack[] itemStacks = tf(player).inventory.mainInventory;
            for (net.minecraft.item.ItemStack is : itemStacks) {
                if (is == null || is.getItem() != CustomItemTypes.PRIZE_BOX) {
                    continue;
                }

                Optional<ItemStack> optContained = PrizeBox.getPrizeStack(is);

                if (optContained.isPresent() && optContained.get().getItem() == CustomItemTypes.PHANTOM_HYMN) {
                    drainAll(); // Force away all water
                    floodBlockType = BlockTypes.FLOWING_LAVA;
                    multiplier = multiplier.multiply(new BigDecimal(1.1));
                    break;
                }
            }
            /*for (Inventory slot : player.getInventory().query((ItemType) CustomItemTypes.PRIZE_BOX)) {
                Optional<ItemStack> optStack = slot.peek();
                if (optStack.isPresent()) {
                    Optional<ItemStack> optContained = PrizeBox.getPrizeStack(optStack.get());
                    if (optContained.isPresent() && optContained.get().getItem() == CustomItemTypes.PHANTOM_HYMN) {
                        drainAll(); // Force away all water
                        floodBlockType = BlockTypes.FLOWING_LAVA;
                        break;
                    }
                }
            }*/

            tf(player).inventoryContainer.detectAndSendChanges();
        }
    }

    private void flood() {
        if (System.currentTimeMillis() - startTime >= TimeUnit.SECONDS.toMillis((3 * 60) / playerMod)) {

            for (Location<World> floodBlock : floodBlocks) {
                floodBlock.getExtent().setBlockType(
                        floodBlock.getBlockPosition(),
                        floodBlockType,
                        true,
                        Cause.of(NamedCause.source(SkreePlugin.container()))
                );
            }

            if (System.currentTimeMillis() - lastFlood >= TimeUnit.SECONDS.toMillis(30 / Math.max(1, playerMod))) {

                Vector3i min = flashMemoryRoom.getMinimumPoint();
                Vector3i max = flashMemoryRoom.getMaximumPoint();

                int minX = min.getX();
                int minZ = min.getZ();
                int minY = min.getY();
                int maxX = max.getX();
                int maxZ = max.getZ();
                int maxY = max.getY();

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockType type = getRegion().getExtent().getBlockType(x, y, z);
                            if (type == BlockTypes.AIR) {
                                getRegion().getExtent().setBlockType(
                                        x, y, z,
                                        floodBlockType,
                                        true,
                                        Cause.of(NamedCause.source(SkreePlugin.container()))
                                );
                                break;
                            }
                        }
                    }
                }
                lastFlood = System.currentTimeMillis();
            }
        }
    }

    private void resetFloodType() {
        floodBlockType = BlockTypes.FLOWING_WATER;
    }
}
