package io.github.relvl.appliedequivalence.mapper.impl.recipe

import io.github.relvl.appliedequivalence.Logger
import io.github.relvl.appliedequivalence.mapper.MappingCollector
import io.github.relvl.appliedequivalence.mapper.impl.RecipeMapper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType

open class DefaultRecipeMapper : RecipeMapper.IRecipeMapper {
    override val order: Int = 0
    override val overprice: Float = 1.1f // todo! config

    override fun process(recipeManager: RecipeManager, recipeType: RecipeType<*>): Int {
        val recipes = recipeManager.getAllRecipesFor(recipeType as RecipeType<Recipe<Container>>)
        var found = 0

        recipes
            .filter { it.resultItem.item != Items.AIR && MappingCollector.getValue(it.resultItem) == null && it.ingredients.size > 0 }
            .forEach { recipe ->
                val ingredients = compactIngredients(recipe) ?: return@forEach
                var valueSum = (ingredients.values.sumOf { it } * overprice).toLong()
                if (valueSum < 1) valueSum = 1

                MappingCollector.setValue(recipe.resultItem, valueSum)
                found++
            }

        if (found > 0) {
            Logger.info("${this.javaClass.simpleName} finished mapping with $found values")
        }

        return found
    }

    /** Selects one of different variants of ingredient by minimal value and returnt the value */
    protected open fun selectIngredient(variants: Array<ItemStack>): Map.Entry<ItemStack, Long>? {
        return variants
            .associateWith { MappingCollector.getValue(it) }
            .filter { it.value != null }
            .mapValues { it.value!! }
            .minByOrNull { it.value }
    }

    /** Compacts same ingredients, sum they count and values */
    protected open fun compactIngredients(recipe: Recipe<Container>): Map<ItemStack, Long>? {
        return recipe.ingredients //
            .filter { it.items.isNotEmpty() }
            .map { selectIngredient(it.items) ?: return null } //
            .groupBy { it.key } //
            .entries //
            .associate { g -> g.key.copy().also { it.count = g.value.sumOf { v -> v.key.count } } to g.value.sumOf { it.value } }
    }

}
