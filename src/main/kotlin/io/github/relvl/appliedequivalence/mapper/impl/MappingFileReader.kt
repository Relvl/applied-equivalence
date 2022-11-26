package io.github.relvl.appliedequivalence.mapper.impl

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import io.github.relvl.appliedequivalence.Logger
import io.github.relvl.appliedequivalence.mapper.ItemIdentity
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Items
import java.io.BufferedReader
import java.io.InputStreamReader

class MappingFileReader {
    @Expose
    var comment: String = ""

    @Expose
    var mappings: Array<MappingsStruct> = emptyArray()

    class MappingsStruct {
        @Expose
        var order: Int = 0

        @Expose
        var tags: Map<String, Long> = emptyMap()

        @Expose
        var items: Map<String, Long> = emptyMap()
    }

    companion object {
        private val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

        fun read(resourceManager: ResourceManager): Set<ItemIdentity> {
            // order -> item-tag-key -> aeq-value
            val store: MutableMap<Int, MutableSet<ItemIdentity>> = HashMap()

            resourceManager.listResources("aeq-mappings", { it.endsWith(".json") }).forEach { loc ->
                resourceManager.getResources(loc).forEach { res ->
                    try {
                        Logger.info("Read mappings file: $loc")

                        // Marshall .json files to MappingFileReader instance
                        val mappingFile = res.inputStream.use { istr ->
                            InputStreamReader(istr).use { isr ->
                                BufferedReader(isr).use { br ->
                                    gson.fromJson(br, MappingFileReader::class.java)
                                }
                            }
                        }

                        // Process mapping entries
                        mappingFile.mappings.forEach { mapping ->
                            // Adding exact items
                            store.computeIfAbsent(mapping.order, { HashSet() }).addAll(mapping.items.entries.mapNotNull { entry ->
                                try {
                                    ItemIdentity(Registry.ITEM.get(ResourceLocation(entry.key)).defaultInstance, entry.value, entry.key, loc.namespace)
                                } catch (t: Throwable) {
                                    return@mapNotNull null
                                }
                            })
                            // Adding exploded tags
                            store.computeIfAbsent(mapping.order, { HashSet() }).addAll(
                                mapping.tags.entries.mapNotNull { t ->
                                    try {
                                        val tagKey = TagKey.create(Registry.ITEM_REGISTRY, ResourceLocation(t.key))
                                        val stacks = Registry.ITEM.getTag(tagKey).get().map { it.value().defaultInstance }
                                        stacks.map { stack ->
                                            ItemIdentity(stack, t.value, "#${t.key}", loc.namespace)
                                        }
                                    } catch (t: Throwable) {
                                        return@mapNotNull null
                                    }
                                }.flatten()
                            )
                        }
                    } catch (_: Throwable) {
                    }
                }
            }

            return store.entries.sortedByDescending { it.key }.flatMap { it.value }.filter { it.value > 0 && it.item != Items.AIR }.toSet()
        }
    }
}