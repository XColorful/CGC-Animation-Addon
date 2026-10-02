package dev.xcolorful.cgcanimation.client.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static dev.xcolorful.cgcanimation.client.command.ClientCommandArg.DISABLE;

public class _DisableCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> getClient() {
        return Commands.literal(DISABLE)
                .executes(_DisableCommand::disable);
    }

    private static int disable(CommandContext<CommandSourceStack> context) {
        return Command.SINGLE_SUCCESS;
    }
}
