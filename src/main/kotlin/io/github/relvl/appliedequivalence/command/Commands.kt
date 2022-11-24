package io.github.relvl.appliedequivalence.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.relvl.appliedequivalence.command.impl.CommandRebuildValues
import io.github.relvl.appliedequivalence.command.impl.CommandUpdateValues
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.server.MinecraftServer

enum class Commands(private val cmd: ICommand) {
    UPDATE_VALUES(CommandUpdateValues),
    REBUILD_VALUES(CommandRebuildValues);

    object CommandsRegisterEvent : ServerLifecycleEvents.ServerStarting {
        override fun onServerStarting(server: MinecraftServer) {
            val builder = literal("aeq")
            Commands.values().forEach {
                val subBuilder = literal(it.cmd.name).requires { src: CommandSourceStack -> src.hasPermission(it.cmd.level) }
                it.cmd.addArguments(subBuilder)
                subBuilder.executes { ctx ->
                    it.cmd.call(server, ctx, ctx.source)
                    1
                }
                builder.then(subBuilder)

            }

            server.commands.dispatcher.register(builder)
        }
    }

    interface ICommand {
        val name: String
        val level: Int

        fun addArguments(builder: LiteralArgumentBuilder<CommandSourceStack>) {}
        fun call(server: MinecraftServer, ctx: CommandContext<CommandSourceStack>, sender: CommandSourceStack)
    }
}