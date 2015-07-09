package com.skelril.skree.service.internal.world;

import com.google.common.base.Optional;
import com.skelril.skree.service.WorldService;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.World;

/**
 * Created by cow_fu on 7/1/15 at 5:19 PM
 */
public class WorldCommandList implements CommandExecutor {

    private Game game;

    public WorldCommandList(Game game){
        this.game = game;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        src.sendMessage(Texts.of("Available worlds (click to teleport)"));

        Optional<WorldService> service = game.getServiceManager().provide(WorldService.class);

        for(WorldEffectWrapper wrapper: service.get().getEffectWrappers()){
            String worldType = wrapper.getName();
            for (World world:wrapper.getWorlds()){
                TextBuilder builder = Texts.builder();
                String worldName = world.getName();
                builder.append(Texts.of(worldName+" ["+worldType+"]"));
                builder.color(TextColors.GREEN);
                builder.onClick(TextActions.runCommand("/world " + worldName));
                builder.onHover(TextActions.showText(Texts.of("Teleport to " + worldName)));
                src.sendMessage(builder.build());
            }
        }
        return CommandResult.success();
    }

    public static CommandSpec ListWorlds(Game game){
        return CommandSpec.builder()
            .description(Texts.of("Teleport to a different world"))
            .permission("skree.world")
            .executor(new WorldCommandList(game)).build();
    }
}
