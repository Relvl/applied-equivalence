package io.github.relvl.appliedequivalence.network

import io.github.relvl.appliedequivalence.AppliedEquivalence
import io.github.relvl.appliedequivalence.network.impl.PckInitializeClient
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import kotlin.reflect.KClass

abstract class AbstractPacket {
    protected val id = PacketLookup.of(this::class)
    protected var buff: FriendlyByteBuf

    constructor() {
        buff = FriendlyByteBuf(Unpooled.buffer())
        buff.writeInt(id?.ordinal ?: -1)
    }

    constructor(buff: FriendlyByteBuf) {
        this.buff = buff
    }

    private fun prepareWrite() {
        val size = buff.readableBytes()
        buff.capacity(size)
        if (size > 2 * 1024 * 1024) {
            throw IllegalArgumentException(
                "Wrong packet size: $size byte(s), Class: ${this::class.simpleName}!"
            )
        }
    }

    fun sendToServer() {
        prepareWrite()
        ClientPlayNetworking.send(CHANNEL_ID, buff)
    }

    fun sentToClient(player: ServerPlayer) {
        prepareWrite()
        ServerPlayNetworking.send(player, CHANNEL_ID, buff)
    }

    enum class PacketLookup(private val clazz: KClass<out AbstractPacket>, val constructor: (FriendlyByteBuf) -> AbstractPacket, val side: EnvType) {
        INIT_CLIENT(PckInitializeClient::class, { buf -> PckInitializeClient(buf) }, EnvType.SERVER);


        companion object {
            private val reverse = values().associateBy { it.clazz }
            fun of(id: Int) = values()[id]
            fun of(id: KClass<out AbstractPacket>) = reverse[id]
        }
    }

    companion object {
        val CHANNEL_ID = ResourceLocation("${AppliedEquivalence.MOD_ID}:networking")
        val PROTOCOL = PacketLookup.values().joinToString("_") { it.name }.hashCode()

        fun onServerPacket(client: Minecraft, handler: ClientPacketListener, buff: FriendlyByteBuf, responseSender: PacketSender) {
            val packetId = buff.readInt()
            val packet = PacketLookup.of(packetId)
            packet.constructor(buff)
        }

        fun onClientPacket(server: MinecraftServer, player: ServerPlayer, handler: ServerGamePacketListenerImpl, buff: FriendlyByteBuf, responseSender: PacketSender) {
            val packetId = buff.readInt()
            val packet = PacketLookup.of(packetId)
            packet.constructor(buff)
        }
    }
}