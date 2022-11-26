package io.github.relvl.appliedequivalence.mapper.impl

import appeng.recipes.handlers.InscriberRecipe
import io.github.relvl.appliedequivalence.Logger
import io.github.relvl.appliedequivalence.mapper.ItemIdentity
import io.github.relvl.appliedequivalence.mapper.impl.recipe.DefaultRecipeMapper
import io.github.relvl.appliedequivalence.mapper.impl.recipe.InscriverRecipeMapper
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType

object RecipeMapper {
    val defaultMapper = DefaultRecipeMapper()
    private val mappers: MutableMap<Int, MutableMap<RecipeType<*>, IRecipeMapper>> = HashMap()

    fun registerMapper(order: Int, type: RecipeType<*>, mapper: IRecipeMapper) {
        mappers.computeIfAbsent(order, { HashMap() })[type] = mapper
    }

    init {
        registerMapper(100, RecipeType.CRAFTING, defaultMapper)
        registerMapper(200, RecipeType.STONECUTTING, defaultMapper)
        registerMapper(300, RecipeType.SMELTING, defaultMapper)
        registerMapper(400, RecipeType.BLASTING, defaultMapper)
        registerMapper(500, RecipeType.SMOKING, defaultMapper)
        registerMapper(600, RecipeType.CAMPFIRE_COOKING, defaultMapper)
        registerMapper(700, InscriberRecipe.TYPE, InscriverRecipeMapper())

    }

    fun process(recipeManager: RecipeManager): Collection<ItemIdentity> {
        val result = HashMap<Int, MutableList<ItemIdentity>>()
        var iterations = 0
        var changed: Boolean
        var found = 0

        do {
            iterations++
            changed = false
            mappers.entries
                .sortedBy { it.key }
                .map { it.value }
                .forEach {
                    it.entries.forEach { entry ->
                        val mappingsFound = entry.value.process(recipeManager, entry.key)
                        changed = changed || mappingsFound > 0
                        found += mappingsFound
                    }
                }
        } while (changed)

        Logger.info("Mapping finished with $found values in $iterations full interations")

        return result.entries.sortedBy { it.key }.flatMap { it.value }.toSet()
    }

    interface IRecipeMapper {
        val order: Int
        val overprice: Float

        fun process(recipeManager: RecipeManager, recipeType: RecipeType<*>): Int
    }
}