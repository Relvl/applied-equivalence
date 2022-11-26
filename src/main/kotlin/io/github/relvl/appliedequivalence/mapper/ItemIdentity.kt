package io.github.relvl.appliedequivalence.mapper

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.*

class ItemIdentity {
    val item: Item
    val tag: CompoundTag?
    val value: Long

    val namespace: String
    val source: String

    constructor(id: Int, tag: CompoundTag?, value: Long, source: String, ns: String) {
        this.item = Item.byId(id)
        this.tag = tag
        this.value = value
        this.namespace = ns
        this.source = source
    }

    constructor(stack: ItemStack, value: Long, source: String) {
        this.item = stack.item
        this.tag = stack.tag
        this.value = value
        this.namespace = "fake"
        this.source = source
    }

    constructor(stack: ItemStack, value: Long, source: String, ns: String) {
        this.item = stack.item
        this.tag = stack.tag
        this.value = value
        this.namespace = ns
        this.source = source
    }

    override fun equals(other: Any?) = this === other || (other is ItemIdentity && item == other.item && Objects.equals(this.tag, other.tag))
    override fun hashCode(): Int {
        var result = item.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "$item($value as '$namespace' @ '$source')"
}