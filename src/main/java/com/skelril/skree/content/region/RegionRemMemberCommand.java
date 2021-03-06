/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.region;

import com.skelril.skree.service.RegionService;
import com.skelril.skree.service.internal.region.Region;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Identifiable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.spongepowered.api.command.args.GenericArguments.allOf;
import static org.spongepowered.api.command.args.GenericArguments.user;

public class RegionRemMemberCommand implements CommandExecutor {
  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

    if (!(src instanceof Player)) {
      src.sendMessage(Text.of("You must be a player to use this command (for now ;) )!"));
      return CommandResult.empty();
    }

    Optional<RegionService> optService = Sponge.getServiceManager().provide(RegionService.class);
    if (!optService.isPresent()) {
      src.sendMessage(Text.of(TextColors.DARK_RED, "The region service is not currently running."));
      return CommandResult.empty();
    }

    RegionService service = optService.get();

    Player player = (Player) src;

    Optional<Region> optRef = service.getSelectedRegion(player);
    if (!optRef.isPresent()) {
      player.sendMessage(Text.of(TextColors.RED, "You do not currently have a region selected."));
      return CommandResult.empty();
    }

    Region ref = optRef.get();

    if (!ref.getMembers().contains(player.getUniqueId())) {
      player.sendMessage(Text.of(TextColors.RED, "You must be a member of the region to modify it!"));
      return CommandResult.empty();
    }

    List<UUID> oldMembers = args.<User>getAll("player").stream().map(Identifiable::getUniqueId).filter(
        a -> ref.getMembers().contains(a)
    ).collect(Collectors.toList());

    ref.remMember(oldMembers);

    player.sendMessage(Text.of(TextColors.YELLOW, "Removed ", oldMembers.size(), " players from the region."));

    return CommandResult.success();
  }

  public static CommandSpec aquireSpec() {
    return CommandSpec.builder()
        .description(Text.of("Remove a player from a region"))
        .arguments(allOf(user(Text.of("player"))))
        .executor(new RegionRemMemberCommand())
        .build();
  }
}
