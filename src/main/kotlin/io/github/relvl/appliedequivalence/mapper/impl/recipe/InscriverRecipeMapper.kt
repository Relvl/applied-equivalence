package io.github.relvl.appliedequivalence.mapper.impl.recipe

import appeng.recipes.handlers.InscriberProcessType
import appeng.recipes.handlers.InscriberRecipe
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe

class InscriverRecipeMapper : DefaultRecipeMapper() {
    override val order: Int = 0
    override val overprice: Float = 1.5f // todo! config

    override fun compactIngredients(recipe: Recipe<Container>): Map<ItemStack, Long>? {
        if (recipe is InscriberRecipe && recipe.processType == InscriberProcessType.INSCRIBE) {
            val selected = selectIngredient(recipe.middleInput.items) ?: return null
            return mapOf(selected.key to selected.value)
        } else {
            return super.compactIngredients(recipe)
        }
    }
}
