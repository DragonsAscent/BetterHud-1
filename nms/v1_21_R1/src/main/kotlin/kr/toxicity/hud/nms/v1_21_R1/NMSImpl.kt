package kr.toxicity.hud.nms.v1_21_R1

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.hud.api.BetterHud
import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.api.nms.NMS
import kr.toxicity.hud.api.nms.NMSVersion
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtOps
import net.minecraft.network.Connection
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.BossEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.WorldBorder
import org.bukkit.boss.BarColor
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer
import org.bukkit.craftbukkit.util.CraftChatMessage
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.PlayerInventory
import org.bukkit.permissions.Permission
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class NMSImpl: NMS {
    companion object {
        private const val INJECT_NAME = BetterHud.DEFAULT_NAMESPACE
        private val bossBarMap = ConcurrentHashMap<UUID, PlayerBossBar>()

        @Suppress("UNCHECKED_CAST")
        private val operation = ClientboundBossEventPacket::class.java.declaredClasses.first {
            it.isEnum
        } as Class<out Enum<*>>

        private val operationEnum = operation.enumConstants
        private val getConnection: (ServerCommonPacketListenerImpl) -> Connection = if (BetterHud.getInstance().isPaper) {
            {
                it.connection
            }
        } else {
            ServerCommonPacketListenerImpl::class.java.declaredFields.first { f ->
                f.type == Connection::class.java
            }.apply {
                isAccessible = true
            }.let { get ->
                {
                    get[it] as Connection
                }
            }
        }

        fun createBossBar(byteBuf: RegistryFriendlyByteBuf): ClientboundBossEventPacket = ClientboundBossEventPacket.STREAM_CODEC.decode(byteBuf)

        private fun toAdventure(component: net.minecraft.network.chat.Component) = GsonComponentSerializer.gson().deserialize(CraftChatMessage.toJSON(component))
        private fun fromAdventure(component: Component) = CraftChatMessage.fromJSON(GsonComponentSerializer.gson().serialize(component))
        private fun getColor(color: BarColor) =  when (color) {
            BarColor.PINK -> BossEvent.BossBarColor.PINK
            BarColor.BLUE -> BossEvent.BossBarColor.BLUE
            BarColor.RED -> BossEvent.BossBarColor.RED
            BarColor.GREEN -> BossEvent.BossBarColor.GREEN
            BarColor.YELLOW -> BossEvent.BossBarColor.YELLOW
            BarColor.PURPLE -> BossEvent.BossBarColor.PURPLE
            BarColor.WHITE -> BossEvent.BossBarColor.WHITE
        }
    }

    override fun inject(player: Player, color: BarColor) {
        player as CraftPlayer
        bossBarMap.computeIfAbsent(player.uniqueId) {
            PlayerBossBar(player, player.handle.connection, color, Component.empty())
        }
    }
    override fun showBossBar(player: Player, color: BarColor, component: Component) {
        bossBarMap[player.uniqueId]?.update(color, component)
    }

    override fun removeBossBar(player: Player) {
        bossBarMap.remove(player.uniqueId)?.remove()
    }

    override fun reloadBossBar(player: Player, color: BarColor) {
        bossBarMap[player.uniqueId]?.resetDummy(color)
    }

    override fun getVersion(): NMSVersion {
        return NMSVersion.V1_21_R1
    }

    override fun getTextureValue(player: Player): String {
        return (player as CraftPlayer).handle.gameProfile.properties.get("textures").first().value
    }

    override fun getFoliaAdaptedPlayer(player: Player): Player {
        val handle = (player as CraftPlayer).handle
        return object : CraftPlayer(Bukkit.getServer() as CraftServer, handle) {
            override fun getPersistentDataContainer(): CraftPersistentDataContainer {
                return player.persistentDataContainer
            }
            override fun getHandle(): ServerPlayer {
                return handle
            }
            override fun getHealth(): Double {
                return player.health
            }
            override fun getScaledHealth(): Float {
                return player.scaledHealth
            }
            override fun getFirstPlayed(): Long {
                return player.firstPlayed
            }
            override fun getInventory(): PlayerInventory {
                return player.inventory
            }
            override fun getEnderChest(): Inventory {
                return player.enderChest
            }
            override fun isOp(): Boolean {
                return player.isOp
            }
            override fun getGameMode(): GameMode {
                return player.gameMode
            }
            override fun getEquipment(): EntityEquipment {
                return player.equipment
            }
            override fun hasPermission(name: String): Boolean {
                return player.hasPermission(name)
            }
            override fun hasPermission(perm: Permission): Boolean {
                return player.hasPermission(perm)
            }
            override fun isPermissionSet(name: String): Boolean {
                return player.isPermissionSet(name)
            }
            override fun isPermissionSet(perm: Permission): Boolean {
                return player.isPermissionSet(perm)
            }
            override fun hasPlayedBefore(): Boolean {
                return player.hasPlayedBefore()
            }
            override fun getWorldBorder(): WorldBorder? {
                return player.getWorldBorder()
            }
            override fun showBossBar(bar: BossBar) {
                player.showBossBar(bar)
            }
            override fun hideBossBar(bar: BossBar) {
                player.hideBossBar(bar)
            }
            override fun sendMessage(message: String) {
                player.sendMessage(message)
            }
            override fun getLastDamageCause(): EntityDamageEvent? {
                return player.lastDamageCause
            }
            override fun pointers(): Pointers {
                return player.pointers()
            }
            override fun spigot(): Player.Spigot {
                return player.spigot()
            }
        }
    }


    private class CachedHudBossbar(val hud: HudBossBar, val cacheUUID: UUID, val buf: HudByteBuf)
    private class PlayerBossBar(val player: Player, val listener: ServerGamePacketListenerImpl, color: BarColor, component: Component): ChannelDuplexHandler() {
        private inner class PlayerDummyBossBar(color: BarColor) {
            val line = BetterHud.getInstance().configManager.bossbarLine - 1
            val dummyBars = (0..<line).map {
                HudBossBar(UUID.randomUUID(), Component.empty(), color).apply {
                    listener.send(ClientboundBossEventPacket.createAddPacket(this))
                }
            }
            val dummyBarsUUID = dummyBars.map {
                it.uuid
            }
        }
        private var dummy = PlayerDummyBossBar(color)
        private val dummyBarHandleMap = Collections.synchronizedMap(LinkedHashMap<UUID, CachedHudBossbar>())
        private val otherBarCache = ConcurrentLinkedQueue<Pair<UUID, HudByteBuf>>()
        private val uuid = UUID.randomUUID().apply {
            listener.send(ClientboundBossEventPacket.createAddPacket(HudBossBar(this, component, color)))
        }

        private var last: HudBossBar = HudBossBar(uuid, Component.empty(), BarColor.RED)
        private var onUse = uuid to HudByteBuf(Unpooled.buffer())

        init {
            val pipeLine = getConnection(listener).channel.pipeline()
            pipeLine.toMap().forEach {
                if (it.value is Connection) pipeLine.addBefore(it.key, INJECT_NAME, this)
            }
        }

        fun update(color: BarColor, component: Component) {
            val bossBar = HudBossBar(uuid, component, color)
            last = bossBar
            listener.send(ClientboundBossEventPacket.createUpdateNamePacket(bossBar))
        }
        
        fun resetDummy(color: BarColor) {
            listener.send(ClientboundBossEventPacket.createRemovePacket(uuid))
            dummy.dummyBarsUUID.forEach {
                listener.send(ClientboundBossEventPacket.createRemovePacket(it))
            }
            dummy = PlayerDummyBossBar(color)
            dummy.dummyBars.forEach { 
                listener.send(ClientboundBossEventPacket.createAddPacket(it))
            }
            listener.send(ClientboundBossEventPacket.createAddPacket(last))
        }

        fun remove() {
            val channel = getConnection(listener).channel
            channel.eventLoop().submit {
                channel.pipeline().remove(INJECT_NAME)
            }
            listener.send(ClientboundBossEventPacket.createRemovePacket(uuid))
            dummy.dummyBarsUUID.forEach {
                listener.send(ClientboundBossEventPacket.createRemovePacket(it))
            }
        }

        private fun writeBossBar(buf: HudByteBuf, ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
            val originalUUID = buf.readUUID()
            if (originalUUID == uuid || dummy.dummyBarsUUID.contains(originalUUID)) {
                super.write(ctx, msg, promise)
                return
            }
            if (BetterHud.getInstance().isOnReload) return
            val enum = buf.readEnum(operation)

            fun getBuf(targetUUID: UUID = uuid) = HudByteBuf(Unpooled.buffer(1 shl 4))
                .writeUUID(targetUUID)

            fun sendProgress(getBuf: HudByteBuf = getBuf(), targetBuf: HudByteBuf = buf) = listener.send(createBossBar(getBuf
                .writeEnum(operationEnum[2])
                .writeFloat(targetBuf.readFloat())
            ))
            fun sendName(getBuf: HudByteBuf = getBuf(), targetBuf: HudByteBuf = buf) = listener.send(createBossBar(getBuf
                .writeEnum(operationEnum[3])
                .writeComponent(targetBuf.readComponentTrusted())
            ))
            fun sendStyle(getBuf: HudByteBuf = getBuf(), targetBuf: HudByteBuf = buf) = listener.send(createBossBar(getBuf
                .writeEnum(operationEnum[4])
                .writeEnum(targetBuf.readEnum(BossEvent.BossBarColor::class.java))
                .writeEnum(targetBuf.readEnum(BossEvent.BossBarOverlay::class.java)))
            )
            fun sendProperties(getBuf: HudByteBuf = getBuf(), targetBuf: HudByteBuf = buf) = listener.send(createBossBar(getBuf
                .writeEnum(operationEnum[5])
                .writeByte(targetBuf.readUnsignedByte().toInt())
            ))
            fun changeName(targetBuf: HudByteBuf = buf) {
                runCatching {
                    val hud = BetterHud.getInstance().getHudPlayer(player)
                    val comp = toAdventure(targetBuf.readComponentTrusted())
                    val key = BetterHud.getInstance().defaultKey
                    fun applyFont(component: Component): Component {
                        return component.font(key).children(component.children().map {
                            applyFont(it)
                        })
                    }
                    @Suppress("DEPRECATION")
                    fun getWidth(component: Component): Int {
                        val style = component.style()
                        return component.children().sumOf {
                            getWidth(it)
                        } + (when (component) {
                            is TextComponent -> component.content()
                            is TranslatableComponent -> BetterHud.getInstance().translate(player.locale, component.key())
                            else -> null
                        }?.codePoints()?.map {
                            var t = BetterHud.getInstance().getWidth(it)
                            if (style.hasDecoration(TextDecoration.BOLD)) t++
                            if (style.hasDecoration(TextDecoration.ITALIC)) t++
                            t + 1
                        }?.sum() ?: 0)
                    }
                    hud.additionalComponent = WidthComponent(Component.text().append(applyFont(comp)), getWidth(comp))
                }
            }
            fun removeBossbar(changeCache: Boolean = false): Boolean {
                if (onUse.first == uuid) return false
                var result = false
                if (changeCache) {
                    val cacheSize = dummyBarHandleMap.size
                    if (cacheSize < dummy.line) {
                        val cache = CachedHudBossbar(dummy.dummyBars[cacheSize], onUse.first, HudByteBuf(onUse.second.unwrap()))
                        dummyBarHandleMap[onUse.first] = cache
                        sendName(getBuf = getBuf(cache.hud.uuid), targetBuf = onUse.second)
                        sendProgress(getBuf = getBuf(cache.hud.uuid), targetBuf = onUse.second)
                        sendStyle(getBuf = getBuf(cache.hud.uuid), targetBuf = onUse.second)
                        sendProperties(getBuf = getBuf(cache.hud.uuid), targetBuf = onUse.second)
                        result = true
                    }
                }
                otherBarCache.poll()?.let { target ->
                    val targetBuf = HudByteBuf(Unpooled.copiedBuffer(target.second.unwrap()))
                    listener.send(ClientboundBossEventPacket.createRemovePacket(target.first))
                    changeName(targetBuf = targetBuf)
                    sendProgress(targetBuf = targetBuf)
                    sendStyle(targetBuf = targetBuf)
                    sendProperties(targetBuf = targetBuf)
                    onUse = target
                } ?: run {
                    onUse = uuid to HudByteBuf(buf.unwrap())
                    BetterHud.getInstance().getHudPlayer(player).additionalComponent = null
                    listener.send(ClientboundBossEventPacket.createUpdateNamePacket(last))
                    listener.send(ClientboundBossEventPacket.createUpdateProgressPacket(last))
                    listener.send(ClientboundBossEventPacket.createUpdateStylePacket(last))
                    listener.send(ClientboundBossEventPacket.createUpdatePropertiesPacket(last))
                }
                return result
            }

            runCatching {
                val cacheSize = dummyBarHandleMap.size
                if (cacheSize < dummy.line && enum.ordinal == 0) {
                    val hud = dummyBarHandleMap.computeIfAbsent(originalUUID) {
                        CachedHudBossbar(dummy.dummyBars[cacheSize], originalUUID, HudByteBuf(buf.unwrap()))
                    }
                    sendName(getBuf = getBuf(hud.hud.uuid))
                    sendProgress(getBuf = getBuf(hud.hud.uuid))
                    sendStyle(getBuf = getBuf(hud.hud.uuid))
                    sendProperties(getBuf = getBuf(hud.hud.uuid))
                    return
                } else {
                    dummyBarHandleMap[originalUUID]?.let {
                        when (enum.ordinal) {
                            0 -> {
                                sendName(getBuf = getBuf(it.hud.uuid))
                                sendProgress(getBuf = getBuf(it.hud.uuid))
                                sendStyle(getBuf = getBuf(it.hud.uuid))
                                sendProperties(getBuf = getBuf(it.hud.uuid))
                            }
                            1 -> {
                                dummyBarHandleMap.remove(originalUUID)
                                val swap = removeBossbar(changeCache = true)
                                val list = dummyBarHandleMap.entries.toList()
                                val last = if (list.isNotEmpty()) list.last().value else it
                                list.forEachIndexed { index, target ->
                                    val after = target.value
                                    val targetBuf = after.buf
                                    val newCache = CachedHudBossbar(dummy.dummyBars[index], after.cacheUUID, HudByteBuf(targetBuf.unwrap()))
                                    target.setValue(newCache)
                                    sendName(getBuf = getBuf(newCache.hud.uuid), targetBuf = targetBuf)
                                    sendProgress(getBuf = getBuf(newCache.hud.uuid), targetBuf = targetBuf)
                                    sendStyle(getBuf = getBuf(newCache.hud.uuid), targetBuf = targetBuf)
                                    sendProperties(getBuf = getBuf(newCache.hud.uuid), targetBuf = targetBuf)
                                }
                                if (!swap) {
                                    listener.send(ClientboundBossEventPacket.createUpdateNamePacket(last.hud))
                                    listener.send(ClientboundBossEventPacket.createUpdateProgressPacket(last.hud))
                                    listener.send(ClientboundBossEventPacket.createUpdateStylePacket(last.hud))
                                    listener.send(ClientboundBossEventPacket.createUpdatePropertiesPacket(last.hud))
                                }
                            }
                            2 -> sendProgress(getBuf = getBuf(it.hud.uuid))
                            3 -> sendName(getBuf = getBuf(it.hud.uuid))
                            4 -> sendStyle(getBuf = getBuf(it.hud.uuid))
                            5 -> sendProperties(getBuf = getBuf(it.hud.uuid))
                            else -> {}
                        }
                        return
                    }
                }
                if (otherBarCache.isEmpty() && enum.ordinal == 0 && onUse.first == uuid) {
                    onUse = originalUUID to HudByteBuf(buf.unwrap())
                    changeName()
                    sendProgress()
                    sendStyle()
                    sendProperties()
                    return
                }
                if (originalUUID == onUse.first) {
                    when (enum.ordinal) {
                        0 -> {
                            changeName()
                            sendProgress()
                            sendStyle()
                            sendProperties()
                        }
                        1 -> removeBossbar()
                        2 -> sendProgress()
                        3 -> changeName()
                        4 -> sendStyle()
                        5 -> sendProperties()
                        else -> {}
                    }
                } else {
                    when (enum.ordinal) {
                        0 -> {
                            otherBarCache.removeIf {
                                it.first == originalUUID
                            }
                            otherBarCache.add(originalUUID to HudByteBuf(buf.unwrap()))
                        }
                        1 -> otherBarCache.removeIf {
                            it.first == originalUUID
                        }
                    }
                    super.write(ctx, msg, promise)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }

        override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
            if (msg is ClientboundBossEventPacket) {

                if (BetterHud.getInstance().isMergeBossBar) {
                    val buf = HudByteBuf(Unpooled.buffer(1 shl 4)).apply {
                        ClientboundBossEventPacket.STREAM_CODEC.encode(this, msg)
                    }
                    writeBossBar(buf, ctx, msg, promise)
                } else super.write(ctx, msg, promise)
            } else {
                super.write(ctx, msg, promise)
            }
        }
    }
    private class HudByteBuf(private val source: ByteBuf): RegistryFriendlyByteBuf(source, RegistryAccess.EMPTY) {
        override fun unwrap(): ByteBuf {
            return Unpooled.copiedBuffer(source)
        }
        override fun writeEnum(instance: Enum<*>): HudByteBuf {
            super.writeEnum(instance)
            return this
        }
        override fun writeUUID(uuid: UUID): HudByteBuf {
            super.writeUUID(uuid)
            return this
        }
        override fun writeFloat(f: Float): HudByteBuf {
            super.writeFloat(f)
            return this
        }
        override fun writeByte(i: Int): HudByteBuf {
            super.writeByte(i)
            return this
        }
        fun readComponentTrusted(): net.minecraft.network.chat.Component {
            return ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, readNbt(NbtAccounter.unlimitedHeap())).orThrow
        }
        fun writeComponent(component: net.minecraft.network.chat.Component): HudByteBuf {
            writeNbt(ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, component).orThrow)
            return this
        }
    }

    private class HudBossBar(val uuid: UUID, component: net.minecraft.network.chat.Component, color: BossBarColor): BossEvent(uuid, component, color, BossBarOverlay.PROGRESS) {
        constructor(uuid: UUID, component: Component, color: BarColor): this(
            uuid,
            fromAdventure(component),
            Companion.getColor(color)
        )
        override fun getProgress(): Float {
            return 0F
        }
    }
}