/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.market;


import com.skelril.skree.service.MarketService;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.remainingJoinedStrings;

public class MarketLookupCommand implements CommandExecutor {

    private Game game;

    public MarketLookupCommand(Game game) {
        this.game = game;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        Optional<MarketService> optService = game.getServiceManager().provide(MarketService.class);
        if (!optService.isPresent()) {
            src.sendMessage(Texts.of(TextColors.DARK_RED, "The market service is not currently running."));
            return CommandResult.empty();
        }

        MarketService service = optService.get();

        Optional<String> optAlias = args.getOne("alias");
        Optional<BigDecimal> optPrice = Optional.empty();
        double percentageSale = 1;

        if (optAlias.isPresent()) {
            optPrice = service.getPrice(optAlias.get());
            optAlias = service.getAlias(optAlias.get());
        } else {
            Optional<ItemStack> held = src instanceof Player ? ((Player) src).getItemInHand() : Optional.empty();
            if (held.isPresent()) {
                optPrice = service.getPrice(held.get());
                optAlias = service.getAlias(held.get());
                net.minecraft.item.ItemStack stack = ((net.minecraft.item.ItemStack) (Object) held.get());
                if (stack.isItemStackDamageable()) {
                    percentageSale = 1 - ((double) stack.getItemDamage() / (double) stack.getMaxDamage());
                }
            }
        }

        if (!optPrice.isPresent()) {
            src.sendMessage(Texts.of(TextColors.DARK_RED, "No valid alias specified, and you're not holding a tracked item."));
            return CommandResult.empty();
        }

        BigDecimal price = optPrice.get();
        BigDecimal sellPrice = price.multiply(service.getSellFactor(price));

        DecimalFormat df = new DecimalFormat("#,###.##");

        String buyPrice = df.format(price);
        String sellUsedPrice = df.format(sellPrice.multiply(new BigDecimal(percentageSale)));
        String sellNewPrice = df.format(sellPrice);

        List<Text> information = new ArrayList<>(6);
        Collections.addAll(
                information,
                Texts.of(TextColors.GOLD, "Price information for: ", TextColors.BLUE, optAlias.get().toUpperCase()),
                Texts.of(TextColors.YELLOW, "When you buy it you pay:"),
                Texts.of(TextColors.YELLOW, " - ", TextColors.WHITE, buyPrice, TextColors.YELLOW, " each."),
                Texts.of(TextColors.YELLOW, "When you sell it you get:"),
                Texts.of(TextColors.YELLOW, " - ", TextColors.WHITE, sellUsedPrice, TextColors.YELLOW, " each.")
        );

        if (percentageSale != 1) {
            information.add(
                    Texts.of(TextColors.YELLOW, " - ", TextColors.WHITE, sellNewPrice, TextColors.YELLOW, " each when new.")
            );
        }

        src.sendMessages(information);
        return CommandResult.success();
    }

    public static CommandSpec aquireSpec(Game game) {
        return CommandSpec.builder()
                .description(Texts.of("Lookup the price information for an item"))
                .arguments(optional(remainingJoinedStrings(Texts.of("alias"))))
                .executor(new MarketLookupCommand(game))
                .build();
    }
}
