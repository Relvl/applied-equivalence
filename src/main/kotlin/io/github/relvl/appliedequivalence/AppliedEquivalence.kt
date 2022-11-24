package io.github.relvl.appliedequivalence

import appeng.api.IAEAddonEntrypoint
import io.github.relvl.appliedequivalence.command.Commands
import io.github.relvl.appliedequivalence.mapper.MapperManager
import io.github.relvl.appliedequivalence.network.AbstractPacket
import io.github.relvl.appliedequivalence.registries.impl.EquivalenceController
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

object AppliedEquivalence : ModInitializer, IAEAddonEntrypoint {
    const val MOD_ID = "applied-equivalence"

    val creativeTab = FabricItemGroupBuilder.build(ResourceLocation(MOD_ID, "item_group")) { ItemStack.EMPTY }!!

    override fun onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(AbstractPacket.CHANNEL_ID, AbstractPacket.Companion::onClientPacket)

        ServerLifecycleEvents.SERVER_STARTING.register(Commands.CommandsRegisterEvent)
        ServerLifecycleEvents.SERVER_STARTING.register(MapperManager)
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(MapperManager)
    }

    override fun onAe2Initialized() {
        EquivalenceController
    }

}
