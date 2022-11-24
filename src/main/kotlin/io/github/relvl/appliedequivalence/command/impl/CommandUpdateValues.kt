package io.github.relvl.appliedequivalence.command.impl

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.relvl.appliedequivalence.command.Commands
import io.github.relvl.appliedequivalence.mapper.MapperManager
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.MinecraftServer

object CommandUpdateValues : Commands.ICommand {
    override val name: String = "update-values"
    override val level: Int = 0

    override fun addArguments(builder: LiteralArgumentBuilder<CommandSourceStack>) {}

    override fun call(server: MinecraftServer, ctx: CommandContext<CommandSourceStack>, sender: CommandSourceStack) {
        try {
            MapperManager.onSyncDataPackContents(sender.playerOrException, true)
            sender.sendSuccess(TextComponent("AEq values update sent"), true)
        } catch (t: Throwable) {
            sender.sendFailure(TextComponent("Can't update AEq values to sender: ${sender.textName}"))
        }
    }
}