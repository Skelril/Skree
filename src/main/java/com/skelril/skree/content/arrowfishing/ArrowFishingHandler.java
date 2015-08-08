package com.skelril.skree.content.arrowfishing;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.skelril.nitro.droptable.DropTable;
import com.skelril.nitro.droptable.DropTableEntryImpl;
import com.skelril.nitro.droptable.DropTableImpl;
import com.skelril.nitro.droptable.resolver.SimpleDropResolver;
import com.skelril.nitro.droptable.roller.SlipperySingleHitDiceRoller;
import com.skelril.nitro.extractor.WorldFromExtent;
import com.skelril.nitro.item.ItemDropper;
import com.skelril.nitro.modifier.ModifierFunctions;
import com.skelril.nitro.probability.Probability;
import com.skelril.nitro.registry.block.MultiTypeRegistry;
import com.skelril.skree.service.ModifierService;
import com.skelril.skree.service.internal.projectilewatcher.ProjectileTickEvent;
import com.skelril.skree.service.internal.projectilewatcher.TrackedProjectileInfo;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.projectile.Arrow;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStackBuilder;
import org.spongepowered.api.world.Location;

import static com.skelril.skree.content.modifier.Modifiers.UBER_ARROW_FISHING;
import static com.skelril.skree.content.registry.item.CustomItemTypes.RAW_GOD_FISH;

/**
 * Created by cow_fu on 7/11/15 at 7:02 PM
 */
public class ArrowFishingHandler {
    private static WorldFromExtent toWorld = new WorldFromExtent();

    private DropTable dropTable;

    public ArrowFishingHandler(Game game) {
        ItemStackBuilder builder = game.getRegistry().getItemBuilder();
        SlipperySingleHitDiceRoller slipRoller = new SlipperySingleHitDiceRoller(ModifierFunctions.MULTI);
        dropTable = new DropTableImpl(
                slipRoller,
                Lists.newArrayList(
                        new DropTableEntryImpl(
                                new SimpleDropResolver(
                                        Lists.newArrayList(
                                                builder.reset().itemType(ItemTypes.FISH).quantity(1).build()
                                        )
                                ),
                                32
                        ),
                        new DropTableEntryImpl(
                                new SimpleDropResolver(
                                        Lists.newArrayList(
                                                builder.reset().itemType((ItemType) RAW_GOD_FISH).quantity(1).build()
                                        )
                                ),
                                500
                        )
                )
        );
    }

    @Subscribe
    public void onProjectileTickEvent(ProjectileTickEvent event) {

        if (!(event.getEntity() instanceof Arrow) || Probability.getChance(3)) {
            return;
        }

        Location loc = event.getEntity().getLocation();
        TrackedProjectileInfo info = event.getProjectileInfo();

        if (info.getProjectileSource().isPresent() && MultiTypeRegistry.isWater(loc.getBlockType())) {
            ProjectileSource source = info.getProjectileSource().get();
            double modifier = 1;

            if (source instanceof Living) {
                Optional<ModifierService> optService = event.getGame().getServiceManager().provide(ModifierService.class);

                if (optService.isPresent() && optService.get().isActive(UBER_ARROW_FISHING)) {
                    modifier *= 2;
                } else {
                    modifier *= 1.25;
                }
            }

            ItemDropper dropper = new ItemDropper(event.getGame(), toWorld.from(loc.getExtent()), loc.getPosition());
            dropper.dropItems(dropTable.getDrops(1, modifier));
        }
    }
}
