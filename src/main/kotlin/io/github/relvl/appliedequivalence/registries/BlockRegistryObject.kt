package io.github.relvl.appliedequivalence.registries

import io.github.relvl.appliedequivalence.AppliedEquivalence
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

interface BlockRegistryObject {
    val id: ResourceLocation
    val block: Block
    val item: BlockItem
    val entityType: BlockEntityType<out BlockEntity>

    companion object {
        fun regId(id: String): ResourceLocation = ResourceLocation(AppliedEquivalence.MOD_ID, id)
    }
}