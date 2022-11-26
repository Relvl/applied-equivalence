package io.github.relvl.appliedequivalence.network.impl

import io.github.relvl.appliedequivalence.mapper.ItemIdentity
import io.github.relvl.appliedequivalence.mapper.MapperManager
import io.github.relvl.appliedequivalence.network.AbstractPacket
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import java.net.ProtocolException

class PckInitializeClient : AbstractPacket {

    companion object {
        private const val pckLimit = 1024 * 1024

        fun chunkAndSend(values: Collection<ItemIdentity>, player: ServerPlayer) {
            var buff = FriendlyByteBuf(Unpooled.buffer())
            var count = 0
            var pckIndex = 0

            fun send() {
                buff.capacity(buff.readableBytes())
                val pck = PckInitializeClient(count, buff, pckIndex++)
                pck.sentToClient(player)
                buff = FriendlyByteBuf(Unpooled.buffer())
                count = 0
            }

            values.forEach { info ->
                if (buff.readableBytes() > pckLimit) {
                    send()
                }
                buff.writeVarInt(Item.getId(info.item))
                buff.writeNbt(info.tag)
                buff.writeLong(info.value)
                buff.writeUtf(info.source)
                buff.writeUtf(info.namespace)
                count++
            }

            send()
        }
    }

    private constructor(count: Int, b: FriendlyByteBuf, pckIndex: Int) : super() {
        buff.writeInt(PROTOCOL)
        buff.writeInt(pckIndex)
        buff.writeInt(count)
        buff.writeBytes(b)
    }

    @Environment(EnvType.CLIENT)
    constructor(buff: FriendlyByteBuf) : super(buff) {
        val protocol = buff.readInt()
        if (PROTOCOL != protocol) {
            throw ProtocolException("Applied Equivalence: different protocol!")
        }
        val pckIndex = buff.readInt()
        val size = buff.readInt()
        val values = HashSet<ItemIdentity>()
        for (i in 0 until size) {
            values.add(
                ItemIdentity(
                    buff.readVarInt(),
                    buff.readNbt(),
                    buff.readLong(),
                    buff.readUtf(),
                    buff.readUtf()
                )
            )
        }
        MapperManager.onClientReveivedValues(values, pckIndex)
    }

}