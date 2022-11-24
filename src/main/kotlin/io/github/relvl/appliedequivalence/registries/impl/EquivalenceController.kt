package io.github.relvl.appliedequivalence.registries.impl

import appeng.api.inventories.InternalInventory
import appeng.api.networking.GridFlags
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.ticking.IGridTickable
import appeng.api.networking.ticking.TickRateModulation
import appeng.api.networking.ticking.TickingRequest
import appeng.api.util.AECableType
import appeng.block.AEBaseBlock
import appeng.block.AEBaseBlockItem
import appeng.block.AEBaseEntityBlock
import appeng.blockentity.grid.AENetworkPowerBlockEntity
import appeng.blockentity.networking.ControllerBlockEntity
import appeng.util.InteractionUtil
import io.github.relvl.appliedequivalence.AppliedEquivalence
import io.github.relvl.appliedequivalence.Logger
import io.github.relvl.appliedequivalence.registries.BlockRegistryObject
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Registry
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.Material
import net.minecraft.world.phys.BlockHitResult
import java.util.*

object EquivalenceController : BlockRegistryObject {
    private val chatUuid = UUID.randomUUID()
    private val P_CONTROLLER_ONLINE: BooleanProperty = BooleanProperty.create("online")


    override val id = BlockRegistryObject.regId("controller")

    override val block = Registry.register(Registry.BLOCK, id, ControllerBlock(AEBaseBlock.defaultProps(Material.METAL)))!!

    override val item = Registry.register(Registry.ITEM, id, ControllerItem(FabricItemSettings().also {
        it.group(AppliedEquivalence.creativeTab)
        it.maxCount(64)
    }))!!

    override val entityType = Registry.register(Registry.BLOCK_ENTITY_TYPE, id, FabricBlockEntityTypeBuilder.create(::ControllerEntity, block).build())!!.also {
        block.setBlockEntity(ControllerEntity::class.java, it, null, null)
    }

    class ControllerItem(props: Properties) : AEBaseBlockItem(block, props) {
        override fun place(context: BlockPlaceContext): InteractionResult {
            when (checkControllerPos(context.level, context.clickedPos)) {
                ControllerChecksResult.NO_CONTROLLER -> {
                    if (!context.level.isClientSide) {
                        context.player?.sendMessage(TextComponent("You should place this block right next to AE Controller!"), chatUuid)
                    }
                    return InteractionResult.FAIL
                }

                // this may be called only on the server side =(
                ControllerChecksResult.HAS_AEO_CONTROLLER -> {
                    context.player?.sendMessage(TextComponent("This AE network already has an Equivalence Controller. No more one."), chatUuid)
                    return InteractionResult.FAIL
                }

                ControllerChecksResult.DIFF_GRID -> {
                    context.player?.sendMessage(TextComponent("You are trying to install a controller to two networks at once. It won't work that way."), chatUuid)
                    return InteractionResult.FAIL
                }

                else -> {
                    // I dunno how to prevent it on the client... If i refuse it any case - item will be placed without sound etc.
                    return super.place(context)
                }
            }
        }
    }

    class ControllerBlock(props: Properties) : AEBaseEntityBlock<ControllerEntity>(props) {
        init {
            registerDefaultState(
                defaultBlockState()
                    .setValue(P_CONTROLLER_ONLINE, false)
            )
        }

        override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
            super.createBlockStateDefinition(builder)
            builder.add(P_CONTROLLER_ONLINE)
        }

        override fun onActivated(level: Level, pos: BlockPos, player: Player, hand: InteractionHand, heldItem: ItemStack?, hit: BlockHitResult): InteractionResult {
            if (InteractionUtil.isInAlternateUseMode(player)) {
                return InteractionResult.PASS
            }
            if (!level.isClientSide) {
                // todo! random lols - this block should does nothing else
                player.sendMessage(TextComponent("Beep Boop!"), chatUuid)
                Logger.info("Controller online: ${level.getBlockState(pos).getValue(P_CONTROLLER_ONLINE)}")
            }
            return InteractionResult.CONSUME
        }

        override fun neighborChanged(blockState: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean) {
            if (block is appeng.block.networking.ControllerBlock) {
                if (checkControllerPos(level, pos) != ControllerChecksResult.GOOD) {
                    level.destroyBlock(pos, true)
                }
            }
        }
    }

    class ControllerEntity(pos: BlockPos, state: BlockState) : AENetworkPowerBlockEntity(entityType, pos, state), IGridTickable {
        init {
            mainNode.setIdlePowerUsage(100.0).setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.PREFERRED, GridFlags.CANNOT_CARRY)
                .setExposedOnSides(EnumSet.noneOf(Direction::class.java)).addService(IGridTickable::class.java, this).setVisualRepresentation(block)
            internalMaxPower = 1600.0
        }

        override fun getCableConnectionType(dir: Direction?) = AECableType.DENSE_COVERED

        override fun onChangeInventory(inv: InternalInventory?, slot: Int) {}
        override fun getInternalInventory(): InternalInventory = InternalInventory.empty()

        override fun getTickingRequest(node: IGridNode): TickingRequest {
            return TickingRequest(1, 20, false, true)
        }

        override fun tickingRequest(node: IGridNode, ticksSinceLastCall: Int): TickRateModulation {
            return TickRateModulation.SAME
        }

        override fun onReady() {
            updateState()
            super.onReady()
        }

        override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
            updateState()
        }

        private fun updateState() {
            level?.let { level ->
                // Update exposed only to ME controller side
                mainNode.setExposedOnSides(getNeighbourControllers(level, blockPos).map { it.dir }.toMutableSet())
                // Update visual state
                val online = mainNode.grid?.energyService?.isNetworkPowered ?: false
                if (level.getBlockState(worldPosition).getValue(P_CONTROLLER_ONLINE) != online) {
                    level.setBlockAndUpdate(worldPosition, level.getBlockState(this.worldPosition).setValue(P_CONTROLLER_ONLINE, online))
                }
            }
        }

    }

    // region PRIVATES

    private fun getNeighbourControllers(level: Level, curPos: BlockPos) = Direction.values().mapNotNull { dir ->
        val relPos = curPos.relative(dir)
        val controller = level.getBlockEntity(relPos) as? ControllerBlockEntity ?: return@mapNotNull null
        return@mapNotNull NeighbourController(controller, dir, relPos)
    }.toSet()

    private fun checkControllerPos(level: Level, curPos: BlockPos): ControllerChecksResult {
        val controllers = getNeighbourControllers(level, curPos)
        if (controllers.isEmpty()) {
            return ControllerChecksResult.NO_CONTROLLER
        }
        // Client side does not know about all this grid things... =(
        if (!level.isClientSide) {
            if (controllers.map { it.controller.gridNode?.grid }.toSet().size > 1) {
                return ControllerChecksResult.DIFF_GRID
            }
            val aeqControllers = controllers.firstOrNull()?.controller?.gridNode?.grid?.getMachines(ControllerEntity::class.java)?.firstOrNull { it.blockPos != curPos }
            if (aeqControllers != null) {
                return ControllerChecksResult.HAS_AEO_CONTROLLER
            }
        }
        return ControllerChecksResult.GOOD
    }

    private enum class ControllerChecksResult {
        GOOD, NO_CONTROLLER, DIFF_GRID, HAS_AEO_CONTROLLER
    }

    private data class NeighbourController(val controller: ControllerBlockEntity, val dir: Direction, val pos: BlockPos)

    // endregion
}