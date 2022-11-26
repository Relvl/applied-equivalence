package io.github.relvl.appliedequivalence.mapper

import io.github.relvl.appliedequivalence.Logger
import net.minecraft.world.item.ItemStack
import kotlin.math.max

object MappingCollector {
    private val fixedValues = HashMap<ItemIdentity, Long>()
    private val calcValues = HashMap<ItemIdentity, Long>()

    fun clear() {
        fixedValues.clear()
        calcValues.clear()
    }

    fun setupFixedValues(fixed: Set<ItemIdentity>) {
        fixedValues.clear()
        fixedValues.putAll(fixed.associateWith { it.value })
    }

    fun getValue(stack: ItemStack): Long? {
        val info = ItemIdentity(stack, 0, "")
        val value = fixedValues[info] ?: calcValues[info]
        if (value != null && value > 0) {
            return value * stack.count
        }
        return null
    }

    fun setValue(stack: ItemStack, value: Long): Boolean {
        val singleValue = max(1, value / max(1, stack.count))
        val info = ItemIdentity(stack, singleValue, "--recipe")
        val prev = calcValues[info]
        return if (prev != null) {
            if (prev != singleValue) {
                Logger.warn("------------------- Set value that already calculated! $info (${calcValues[info]} -> $singleValue)")
            }
            false
        } else {
            calcValues[info] = singleValue
            true
        }
    }

    fun finalize(): Map<ItemIdentity, ItemIdentity> {
        val result = HashMap<ItemIdentity, ItemIdentity>()

        result.putAll(calcValues.keys.associateBy { it })
        result.putAll(fixedValues.keys.associateBy { it })

        return result
    }
}