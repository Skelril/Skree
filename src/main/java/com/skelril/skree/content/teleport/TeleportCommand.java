/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.teleport;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.entity.GameModeData;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.player.gamemode.GameModes;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.extent.Extent;

import static org.spongepowered.api.util.command.args.GenericArguments.*;

public class TeleportCommand implements CommandExecutor {
    private final Game game;

    public TeleportCommand(Game game) {
        this.game = game;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player target;
            if (src instanceof Player) {
            target = (Player) src;
        } else {
            src.sendMessage(Texts.of(TextColors.RED, "You are not a player and teleporting other players is not currently supported!"));
            return CommandResult.empty();
        }

        Optional<Vector3d> dest = args.<Vector3d>getOne("dest");
        Vector3d rotation = new Vector3d(0, 0, 0);
        Extent targetExtent = target.getWorld();
        String destStr;

        if (dest.isPresent()) {
            destStr = dest.get().toString();
        } else {
            Player player = args.<Player>getOne("dest-player").get();
            targetExtent = player.getWorld();
            rotation = player.getRotation();
            dest = Optional.of(player.getLocation().getPosition());
            destStr = player.getName();
        }

        Optional<GameModeData> data = target.getData(GameModeData.class);
        if (!data.isPresent() || !(data.get().getGameMode() == GameModes.CREATIVE || data.get().getGameMode() == GameModes.SPECTATOR)) {
            //make sure no lava/fire is at the targets destination
            dest = Optional.of(dest.get().add(0, 2, 0));

            while (dest.get().getFloorY() > 0) {
                dest = Optional.of(dest.get().add(0, -1, 0));

                if (!(targetExtent.getBlock(dest.get().toInt()).getType().equals(BlockTypes.AIR))) {
                    if (targetExtent.getBlock(dest.get().toInt()).getType().equals(BlockTypes.LAVA) || targetExtent.getBlock(dest.get().toInt()).getType().equals(BlockTypes.FLOWING_LAVA) || targetExtent.getBlock(dest.get().toInt()).getType().equals(BlockTypes.FIRE)) {
                        src.sendMessage(Texts.of(TextColors.RED, "My lord, thou would burn thy feet teleporting here!"));
                        return CommandResult.empty();
                    }
                    break;
                }
            }
            dest = Optional.of(target.getLocation().getPosition());

            //make sure target doesn't fall
            while (dest.get().getFloorY() > 0 && targetExtent.getBlock(dest.get().toInt()).getType().equals(BlockTypes.AIR)) {
                dest = Optional.of(dest.get().add(0, -1, 0));
            }

            //make sure target doesn't suffocate
            while (!(targetExtent.getBlock(dest.get().toInt()).getType().equals(BlockTypes.AIR) && dest.get().getFloorY() < 255)) {
                dest = Optional.of(dest.get().add(0, 1, 0));
            }
        }
        target.setLocationAndRotation(new Location(targetExtent, dest.get().add(0, 1, 0)), rotation);

        src.sendMessage(Texts.of(TextColors.YELLOW, "Teleported to " + destStr + '.'));

        return CommandResult.success();
    }

    public static CommandSpec aquireSpec(Game game) {
        return CommandSpec.builder()
                .description(Texts.of("Teleport to a player or destination"))
                .permission("skree.teleport.teleport")
                .arguments(
                        onlyOne(
                                firstParsing(
                                        player(Texts.of("dest-player"), game),
                                        vector3d(Texts.of("dest"))
                                )
                        )
                ).executor(new TeleportCommand(game)).build();
    }
}
