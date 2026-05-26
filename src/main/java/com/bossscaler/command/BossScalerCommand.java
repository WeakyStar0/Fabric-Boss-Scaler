package com.bossscaler.command;

import com.bossscaler.config.BossScalerConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class BossScalerCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("bossscaler")
                .requires(src -> src.hasPermissionLevel(2))
                .then(
                    CommandManager.literal("reload")
                        .executes(ctx -> {
                            BossScalerConfig.reload();
                            ctx.getSource().sendFeedback(
                                () -> Text.literal("[BossScaler] Config reloaded successfully."),
                                true
                            );
                            return 1;
                        })
                )
        );
    }
}
