package io.github.relvl.appliedequivalence.command.impl

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.relvl.appliedequivalence.command.Commands
import io.github.relvl.appliedequivalence.mapper.MapperManager
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.MinecraftServer

object CommandRebuildValues : Commands.ICommand {
    override val name: String = "rebuild-values"
    override val level: Int = 4

    override fun addArguments(builder: LiteralArgumentBuilder<CommandSourceStack>) {}

    override fun call(server: MinecraftServer, ctx: CommandContext<CommandSourceStack>, sender: CommandSourceStack) {
        try {
            sender.sendSuccess(TextComponent("Start building AEq values..."), true)
            MapperManager.onServerStarting(server)
            sender.sendSuccess(TextComponent("AEq values rebuilded"), true)
            server.playerList.players.forEach {
                sender.sendSuccess(TextComponent("Sending update for player '").append(it.name).append("'"), true)
                MapperManager.onSyncDataPackContents(it, true)
            }

        } catch (t: Throwable) {
            sender.sendFailure(TextComponent("Can't rebuild AEq values"))
        }
    }
}