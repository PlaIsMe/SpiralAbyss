package com.pla.spiral_abyss.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;

public final class CommandInit {
    private CommandInit() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spiral_abyss")
                .then(Commands.literal("floor").then(Commands.argument("number", IntegerArgumentType.integer(1, FloorParser.MAX_FLOORS)).executes(c ->
                        AbyssParties.get(c.getSource().getServer()).enterFloor(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "number")) ? 1 : 0)))
                .then(Commands.literal("next_floor").executes(c -> AbyssParties.get(c.getSource().getServer()).nextFloor(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("reset").executes(c -> AbyssParties.get(c.getSource().getServer()).reset(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("leave").executes(c -> AbyssParties.get(c.getSource().getServer()).leave(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player()).executes(c ->
                        AbyssParties.get(c.getSource().getServer()).invite(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")) ? 1 : 0)))
                .then(Commands.literal("join").then(Commands.argument("host", EntityArgument.player()).executes(c ->
                        AbyssParties.get(c.getSource().getServer()).join(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "host")) ? 1 : 0))));
    }
}
