/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.teleport;

import com.flowpowered.math.vector.Vector3d;
import com.skelril.nitro.entity.SafeTeleportHelper;
import com.skelril.skree.content.world.WorldEntryPermissionCheck;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import static org.spongepowered.api.command.args.GenericArguments.*;

public class TeleportCommand implements CommandExecutor {
  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    Player target;
    if (src instanceof Player) {
      target = (Player) src;
    } else {
      src.sendMessage(Text.of(TextColors.RED, "You are not a player and teleporting other players is not currently supported!"));
      return CommandResult.empty();
    }

    Optional<Vector3d> dest = args.getOne("dest");
    Vector3d rotation = new Vector3d(0, 0, 0);
    World targetExtent = target.getWorld();
    String destStr;

    if (dest.isPresent()) {
      if (!src.hasPermission("skree.teleport.teleport.coord")) {
        src.sendMessage(Text.of(TextColors.RED, "You do not have permission to teleport by coordinates here."));
        return CommandResult.empty();
      }
      destStr = dest.get().toString();
    } else {
      if (!src.hasPermission("skree.teleport.teleport.player")) {
        src.sendMessage(Text.of(TextColors.RED, "You do not have permission to teleport to players here."));
        return CommandResult.empty();
      }
      Player player = args.<Player>getOne("dest-player").get();
      targetExtent = player.getWorld();
      rotation = player.getRotation();
      dest = Optional.of(player.getLocation().getPosition());
      destStr = player.getName();
    }

    Optional<Location<World>> optSafeDest = SafeTeleportHelper.getSafeDest(
        target,
        new Location<>(targetExtent, dest.get())
    );

    if (optSafeDest.isPresent()) {
      if (!WorldEntryPermissionCheck.checkDestination(target, optSafeDest.get().getExtent())) {
        src.sendMessage(Text.of(TextColors.RED, "You do not have permission to access worlds of this type."));
        return CommandResult.empty();
      }

      target.setLocationAndRotation(optSafeDest.get(), rotation);

      src.sendMessage(Text.of(TextColors.YELLOW, "Teleported to " + destStr + '.'));
    } else {
      src.sendMessage(Text.of(TextColors.RED, "The requested destination is unsafe."));
    }

    return CommandResult.success();
  }

  public static CommandSpec aquireSpec() {
    return CommandSpec.builder()
        .description(Text.of("Teleport to a player or destination"))
        .arguments(
            onlyOne(
                firstParsing(
                    player(Text.of("dest-player")),
                    vector3d(Text.of("dest"))
                )
            )
        ).executor(new TeleportCommand()).build();
  }
}
