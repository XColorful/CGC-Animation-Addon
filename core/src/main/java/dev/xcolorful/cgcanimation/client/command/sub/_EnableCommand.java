package dev.xcolorful.cgcanimation.client.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static dev.xcolorful.cgcanimation.client.command.ClientCommandArg.ENABLE;

public class _EnableCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> getClient() {
        return Commands.literal(ENABLE)
                .executes(_EnableCommand::enable);
    }

    private static int enable(CommandContext<CommandSourceStack> context) {
        return Command.SINGLE_SUCCESS;
    }
}
