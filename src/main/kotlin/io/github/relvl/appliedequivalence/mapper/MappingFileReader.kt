package io.github.relvl.appliedequivalence.mapper

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import io.github.relvl.appliedequivalence.Logger
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.tags.TagKey
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

    private class FlatStoreElement(val ns: String, val order: Int, val itemKey: String, val aeqValue: Long)

    companion object {
        private val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()

        fun read(resourceManager: ResourceManager): Collection<MapperManager.MappingEntry> {
            // namespace -> order -> item-tag-key -> aeq-value
            val store: MutableMap<String, MutableMap<Int, MutableMap<String, Long>>> = HashMap()

            resourceManager.listResources("aeq-mappings", { it.endsWith(".json") }).forEach { loc ->
                val namespaceStore = store.computeIfAbsent(loc.namespace, { HashMap() })

                resourceManager.getResources(loc).forEach { res ->
                    try {
                        val file = res.inputStream.use { istr ->
                            InputStreamReader(istr).use { isr ->
                                BufferedReader(isr).use { br ->
                                    gson.fromJson(br, MappingFileReader::class.java)
                                }
                            }
                        }

                        Logger.info("Read mappings file: $loc")

                        file.mappings.forEach { m ->
                            namespaceStore.computeIfAbsent(m.order, { HashMap() }).putAll(m.items)

                            val taggedStacks = m.tags.mapKeys { it ->
                                val tagKey = TagKey.create(Registry.ITEM_REGISTRY, ResourceLocation(it.key))
                                Registry.ITEM.getTag(tagKey).get().map { it.value().defaultInstance }
                            }

                            namespaceStore.computeIfAbsent(m.order, { HashMap() }).putAll(m.tags.mapKeys { "#${it.key}" })
                        }
                    } catch (_: Throwable) {
                    }
                }
            }

            val flatStore = store.entries.flatMap { s ->
                s.value.flatMap { o ->
                    o.value.map { maping ->
                        FlatStoreElement(s.key, o.key, maping.key, maping.value)
                    }
                }
            }.sortedBy { it.order }

            return emptyList()
        }
    }
}