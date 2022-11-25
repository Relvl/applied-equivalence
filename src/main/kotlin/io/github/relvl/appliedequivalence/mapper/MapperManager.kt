package io.github.relvl.appliedequivalence.mapper

import io.github.relvl.appliedequivalence.Logger
import io.github.relvl.appliedequivalence.network.impl.PckInitializeClient
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.Registry
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.*

object MapperManager : ServerLifecycleEvents.ServerStarting, ServerLifecycleEvents.SyncDataPackContents {

    private val readedMappings = setOf(
        MappingEntry(100, "minecraft:obsidian", 1024),
        MappingEntry(10000, "minecraft:iron_ingot", 123),
        MappingEntry(1000, "#c:iron_ingots", 100),
        MappingEntry(10, "#ae2:metal_ingots", 1000),
        MappingEntry(10, "#ae2:all_fluix", 1000),
        MappingEntry(10, "#ae2:all_certus_quartz", 1000),
    )

    private val values = HashMap<ItemIdentity, ItemIdentity>()

    fun ofStack(stack: ItemStack): ItemIdentity? {
        val info = ItemIdentity(stack, 0, "")
        return values[info]
    }

    private fun processItemStack(itemStack: ItemStack, mappingEntry: MappingEntry): ItemIdentity {
        Logger.info("--- Process item ${itemStack.descriptionId}, set value: ${mappingEntry.value}")

        return ItemIdentity(itemStack, mappingEntry.value, mappingEntry.name)
    }

    override fun onServerStarting(server: MinecraftServer) {
        val prebuildValues = HashMap<ItemIdentity, ItemIdentity>()

        val mappings = MappingFileReader.read(server.resourceManager)

        readedMappings.sortedBy { it.order }.forEach { mapping ->
            try {
                if (mapping.name.startsWith("#")) {
                    Logger.info("Processing tag ${mapping.name}...")
                    val tagKey = TagKey.create(Registry.ITEM_REGISTRY, ResourceLocation(mapping.name.substring(1)))
                    val itemStacks = Registry.ITEM.getTag(tagKey).get().map { it.value().defaultInstance }
                    itemStacks.forEach {
                        val info = processItemStack(it, mapping)
                        prebuildValues[info] = info
                    }
                } else {
                    Logger.info("Processing item ${mapping.name}")
                    val itemStack = Registry.ITEM.get(ResourceLocation(mapping.name)).defaultInstance
                    val info = processItemStack(itemStack, mapping)
                    prebuildValues[info] = info
                }
            } catch (t: Throwable) {
                Logger.warn("Can not process mapping entry: ${mapping.name}")
            }
        }

        val recipeManager = server.recipeManager

        prebuildValues.entries.removeIf { it.value.value <= 0 || it.value.item == Items.AIR }

        values.clear()
        values.putAll(prebuildValues)
    }

    override fun onSyncDataPackContents(player: ServerPlayer, joined: Boolean) {
        if (joined) {
            PckInitializeClient.chunkAndSend(values.keys, player)
        }
    }

    fun onClientReveivedValues(values: Collection<ItemIdentity>, pckIndex: Int) {
        if (pckIndex == 0) {
            this.values.clear()
        }
        this.values.putAll(values.associateBy { it })
        Logger.info("Client received ${values.size} updated AEq values (pck#$pckIndex)")
    }

    class ItemIdentity {
        val item: Item
        val tag: CompoundTag?
        val value: Long
        val source: String

        constructor(id: Int, tag: CompoundTag?, value: Long, source: String) {
            this.item = Item.byId(id)
            this.tag = tag
            this.value = value
            this.source = source
        }

        constructor(stack: ItemStack, value: Long, source: String) {
            item = stack.item
            tag = stack.tag
            this.value = value
            this.source = source
        }

        override fun equals(other: Any?) = this === other || (other is ItemIdentity && item == other.item && Objects.equals(this.tag, other.tag))
        override fun hashCode(): Int {
            var result = item.hashCode()
            result = 31 * result + (tag?.hashCode() ?: 0)
            return result
        }
    }

    class MappingEntry(val order: Int, val name: String, val value: Long)
}