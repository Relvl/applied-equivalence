package io.github.relvl.appliedequivalence.mapper

import io.github.relvl.appliedequivalence.Logger
import io.github.relvl.appliedequivalence.network.impl.PckInitializeClient
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

object MapperManager : ServerLifecycleEvents.ServerStarting, ServerLifecycleEvents.SyncDataPackContents {
    private val values = HashMap<ItemIdentity, ItemIdentity>()

    fun ofStack(stack: ItemStack): ItemIdentity? {
        val info = ItemIdentity(stack, 0, "")
        return values[info]
    }

    override fun onServerStarting(server: MinecraftServer) {
        val time = System.currentTimeMillis()
        val prebuildValues = HashMap<ItemIdentity, ItemIdentity>()

        prebuildValues.putAll(MappingFileReader.read(server.resourceManager).associateBy { it })

        val recipeManager = server.recipeManager

        values.clear()
        values.putAll(prebuildValues)
        Logger.info("Loading AEq values took ${System.currentTimeMillis() - time} ms")
    }

    override fun onSyncDataPackContents(player: ServerPlayer, joined: Boolean) {
        if (joined) {
            PckInitializeClient.chunkAndSend(values.keys, player)
        }
    }

    @Environment(EnvType.CLIENT)
    fun onClientReveivedValues(values: Collection<ItemIdentity>, pckIndex: Int) {
        if (pckIndex == 0) {
            Logger.info("Clear client AEq values")
            this.values.clear()
        }
        this.values.putAll(values.associateBy { it })
        Logger.info("Client received ${values.size} updated AEq values (pck#$pckIndex)")
    }
}