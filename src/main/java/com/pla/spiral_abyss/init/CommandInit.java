package com.pla.spiral_abyss.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.commands.*;

public final class CommandInit {
    private CommandInit() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spiral_abyss")
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2)).executes(c -> {
                    AbyssParties.get(c.getSource().getServer()).resetGlobal();
                    c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("message.spiral_abyss.global_reset"), true);
                    return 1;
                }))
                .executes(c -> AbyssParties.get(c.getSource().getServer()).portals().openArenaMenu(c.getSource().getPlayerOrException()) ? 1 : 0)
                .then(Commands.literal("floor").then(Commands.argument("number", IntegerArgumentType.integer(1, FloorParser.MAX_FLOORS)).executes(c ->
                        AbyssParties.get(c.getSource().getServer()).enterFloor(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "number")) ? 1 : 0)))
                .then(Commands.literal("next_floor").executes(c -> AbyssParties.get(c.getSource().getServer()).nextFloor(c.getSource().getPlayerOrException()) ? 1 : 0)));
    }
}
