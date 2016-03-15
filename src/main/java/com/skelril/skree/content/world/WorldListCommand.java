/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.world;


import com.skelril.skree.service.WorldService;
import com.skelril.skree.service.internal.world.WorldEffectWrapper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class WorldListCommand implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        src.sendMessage(Text.of(TextColors.GOLD, "Available worlds (click to teleport):"));

        Optional<WorldService> service = Sponge.getServiceManager().provide(WorldService.class);

        for (WorldEffectWrapper wrapper : service.get().getEffectWrappers()) {
            String worldType = wrapper.getName();
            for (World world : wrapper.getWorlds()) {
                Text.Builder builder = Text.builder();
                String worldName = world.getName();
                builder.append(Text.of(worldName + " [" + worldType + "]"));
                builder.color(TextColors.GREEN);
                builder.onClick(TextActions.runCommand("/world " + worldName));
                builder.onHover(TextActions.showText(Text.of("Teleport to " + worldName)));
                src.sendMessage(builder.build());
            }
        }
        return CommandResult.success();
    }

    public static CommandSpec aquireSpec() {
        return CommandSpec.builder()
            .description(Text.of("List available worlds"))
            .permission("skree.world.teleport")
            .executor(new WorldListCommand()).build();
    }
}
