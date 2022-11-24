package io.github.relvl.appliedequivalence.client

import io.github.relvl.appliedequivalence.client.hook.ItemTooltipHook
import io.github.relvl.appliedequivalence.network.AbstractPacket
import io.github.relvl.appliedequivalence.registries.impl.EquivalenceController
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.renderer.RenderType

@Environment(EnvType.CLIENT)
object AppliedEquivalenceClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AbstractPacket.CHANNEL_ID, AbstractPacket.Companion::onServerPacket)

        BlockRenderLayerMap.INSTANCE.putBlock(EquivalenceController.block, RenderType.cutout())
        ItemTooltipCallback.EVENT.register(ItemTooltipHook)
    }
}