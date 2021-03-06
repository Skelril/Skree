/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.modifier;

import com.nearce.gamechatter.sponge.GameChatterPlugin;
import com.skelril.nitro.text.PrettyText;
import com.skelril.skree.service.ModifierService;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.spongepowered.api.command.args.GenericArguments.*;

public class ModExtendCommand implements CommandExecutor {

  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    Optional<ModifierService> optService = Sponge.getServiceManager().provide(ModifierService.class);
    if (!optService.isPresent()) {
      src.sendMessage(Text.of(TextColors.DARK_RED, "Modifier service not found."));
      return CommandResult.empty();
    }
    ModifierService service = optService.get();

    String modifier = args.<String>getOne("modifier").get();
    int minutes = args.<Integer>getOne("minutes").get();

    boolean wasActive = service.isActive(modifier);

    service.extend(modifier, TimeUnit.MINUTES.toMillis(minutes));

    String friendlyName = StringUtils.capitalize(modifier.replace("_", " ").toLowerCase());
    String friendlyTime = PrettyText.date(service.expiryOf(modifier));

    String change = wasActive ? " extended" : " enabled";
    String rawMessage = friendlyName + change + " till " + friendlyTime + "!";

    MessageChannel.TO_ALL.send(Text.of(TextColors.GOLD, rawMessage));
    GameChatterPlugin.inst().sendSystemMessage(rawMessage);

    return CommandResult.success();
  }

  public static CommandSpec aquireSpec() {
    Map<String, String> choices = new HashMap<>();
    for (Field field : Modifiers.class.getFields()) {
      try {
        Object result = field.get(null);
        if (result instanceof String) {
          choices.put((String) result, (String) result);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return CommandSpec.builder()
        .description(Text.of("Extend modifiers"))
        .permission("skree.modifier")
        .arguments(seq(onlyOne(choices(Text.of("modifier"), choices)), onlyOne(integer(Text.of("minutes")))))
        .executor(new ModExtendCommand()).build();
  }
}
