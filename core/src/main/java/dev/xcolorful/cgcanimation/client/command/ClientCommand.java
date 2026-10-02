package dev.xcolorful.cgcanimation.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xcolorful.cgcanimation.client.command.sub._DisableCommand;
import dev.xcolorful.cgcanimation.client.command.sub._EnableCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static dev.xcolorful.cgcanimation.CgcAnimation.MOD_ID;

public class ClientCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(get(MOD_ID));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get(String rootName) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(rootName);
        root.then(_EnableCommand.getClient()
        );
        root.then(_DisableCommand.getClient()
        );
        return root;
    }
}
