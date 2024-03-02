package kr.toxicity.hud.hud

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.image.ImageLocation
import kr.toxicity.hud.image.ImageType
import kr.toxicity.hud.image.ListenerHudImage
import kr.toxicity.hud.image.SplitType
import kr.toxicity.hud.layout.ImageLayout
import kr.toxicity.hud.util.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.io.File
import kotlin.math.ceil
import kotlin.math.round

class HudImageElement(name: String, file: File, private val image: ImageLayout, index: Int, x: Int, y: Int, animation: List<ImageLocation>) {


    private val chars = run {
        val hud = image.image

        val bit = (x shl (Hud.DEFAULT_BIT + 7)) + (y shl Hud.DEFAULT_BIT) + image.y + Hud.AND_BIT
        val maxWidth = hud.image.maxOf {
            it.second.width
        }
        val isSingle = hud.image.size == 1


        animation.mapIndexed { index2, imageLocation ->
            val array = JsonArray()
            val key = Key.key("mythichud:hud/$name/image/${hud.name}_${index + 1}_${index2 + 1}")
            val list = ArrayList<WidthComponent>()
            var i = 0xD0000
            if (hud is ListenerHudImage) {
                list.add(FORWARD_ONE_SPACE_COMPONENT)
            }
            hud.image.forEach { pair ->
                val finalBit = bit //+ (index shl DEFAULT_BIT + 14)
                val c = (++i).parseChar()
                var finalWidth = WidthComponent(Component.text(c).font(key), round(pair.second.width.toDouble() * image.scale).toInt())
                if (hud is ListenerHudImage) {
                    when (hud.splitType) {
                        SplitType.RIGHT -> {
                            finalWidth = (maxWidth - finalWidth.width).toSpaceComponent() + finalWidth
                        }
                        else -> {}
                    }
                }
                array.add(JsonObject().apply {
                    addProperty("type", "bitmap")
                    if (isSingle) addProperty("file", "mythichud:image/${pair.first}")
                    else addProperty("file", "mythichud:image/${hud.name}/${pair.first}")
                    addProperty("ascent", -finalBit - imageLocation.y.coerceAtLeast(-512).coerceAtMost(512))
                    addProperty("height", round(pair.second.height.toDouble() * image.scale).toInt())
                    add("chars", JsonArray().apply {
                        add(c)
                    })
                })
                list.add(image.x.toSpaceComponent() + imageLocation.x.toSpaceComponent() + finalWidth)
            }
            JsonObject().apply {
                add("providers", array)
            }.save(File(file, "${hud.name}_${index + 1}_${index2 + 1}.json"))
            list
        }

    }

    fun getComponent(player: HudPlayer): WidthComponent {
        val getChars = chars[(player.tick % chars.size).toInt()]
        return if (image.image.conditions(player) && image.conditions(player)) image.image.type.getComponent(image.image, getChars, player) + NEGATIVE_ONE_SPACE_COMPONENT + NEW_LAYER else EMPTY_WIDTH_COMPONENT
    }

}