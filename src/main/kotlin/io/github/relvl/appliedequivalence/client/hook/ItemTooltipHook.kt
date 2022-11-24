package io.github.relvl.appliedequivalence.client.hook

import io.github.relvl.appliedequivalence.mapper.MapperManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

@Environment(EnvType.CLIENT)
object ItemTooltipHook : ItemTooltipCallback {
    override fun getTooltip(stack: ItemStack, context: TooltipFlag, lines: MutableList<Component>) {
        MapperManager.ofStack(stack)?.let {
            if (stack.count > 1) {
                if (Screen.hasShiftDown()) {
                    lines.add(TranslatableComponent("Equivalence: %s * %s = %s", it.value, stack.count, it.value * stack.count))
                } else {
                    lines.add(TranslatableComponent("Stack Equivalence: %s", it.value * stack.count))
                }
            } else {
                lines.add(TranslatableComponent("Equivalence: %s", it.value))
            }

            if (Screen.hasShiftDown()) {
                lines.add(TranslatableComponent("AEq added as %s", it.source))
            }
            ChatFormatting.YELLOW
        }
    }
}