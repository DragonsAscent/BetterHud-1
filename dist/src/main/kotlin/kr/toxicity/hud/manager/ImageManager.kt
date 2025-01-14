package kr.toxicity.hud.manager

import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.image.HudImage
import kr.toxicity.hud.image.ImageType
import kr.toxicity.hud.image.SplitType
import kr.toxicity.hud.pack.PackGenerator
import kr.toxicity.hud.resource.GlobalResource
import kr.toxicity.hud.shader.ShaderGroup
import kr.toxicity.hud.util.*
import net.kyori.adventure.audience.Audience
import org.bukkit.configuration.MemoryConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object ImageManager: BetterHudManager {

    private val imageMap = HashMap<String, HudImage>()
    private val emptySetting = MemoryConfiguration()

    private val imageNameComponent = ConcurrentHashMap<ShaderGroup, WidthComponent>()

    @Synchronized
    fun getImage(group: ShaderGroup) = imageNameComponent[group]
    @Synchronized
    fun setImage(group: ShaderGroup, component: WidthComponent) {
        imageNameComponent[group] = component
    }

    override fun start() {
    }

    fun getImage(name: String) = synchronized(imageMap) {
        imageMap[name]
    }

    private val multiFrameRegex = Pattern.compile("(?<name>(([a-zA-Z]|/|.|(_))+)):(?<frame>([0-9]+))")

    override fun reload(sender: Audience, resource: GlobalResource) {
        synchronized(imageMap) {
            imageMap.clear()
            imageNameComponent.clear()
        }
        val assets = DATA_FOLDER.subFolder("assets")
        DATA_FOLDER.subFolder("images").forEachAllYaml(sender) { file, s, configurationSection ->
            runWithExceptionHandling(sender, "Unable to load this image: $s in ${file.name}") {
                val image = when (val type = ImageType.valueOf(
                    configurationSection.getString("type").ifNull("type value not set.").uppercase()
                )) {
                    ImageType.SINGLE -> {
                        val targetFile = File(
                            assets,
                            configurationSection.getString("file").ifNull("file value not set.")
                                .replace('/', File.separatorChar)
                        )
                        HudImage(
                            file.path,
                            s,
                            listOf(
                                targetFile
                                    .toImage()
                                    .removeEmptySide()
                                    .ifNull("Invalid image.")
                                    .toNamed(targetFile.name),
                            ),
                            type,
                            configurationSection.getConfigurationSection("setting") ?: emptySetting
                        )
                    }

                    ImageType.LISTENER -> {
                        val splitType = (configurationSection.getString("split-type")?.let { splitType ->
                            runWithExceptionHandling(sender, "Unable to find that split-type: $splitType") {
                                SplitType.valueOf(splitType.uppercase())
                            }.getOrNull()
                        } ?: SplitType.LEFT)
                        val getFile = File(
                            assets,
                            configurationSection.getString("file").ifNull("file value not set.")
                                .replace('/', File.separatorChar)
                        )
                        HudImage(
                            file.path,
                            s,
                            splitType.split(
                                getFile
                                    .toImage()
                                    .removeEmptySide()
                                    .ifNull("Invalid image.")
                                    .toNamed(getFile.name), configurationSection.getInt("split", 25).coerceAtLeast(1)
                            ),
                            type,
                            configurationSection.getConfigurationSection("setting")
                                .ifNull("setting configuration not found.")
                        )
                    }

                    ImageType.SEQUENCE -> {
                        val globalFrame = configurationSection.getInt("frame", 1).coerceAtLeast(1)
                        HudImage(
                            file.path,
                            s,
                            configurationSection.getStringList("files").ifEmpty {
                                warn("files is empty.")
                                return@runWithExceptionHandling
                            }.map { string ->
                                val matcher = multiFrameRegex.matcher(string)
                                var fileName = string
                                var frame = 1
                                if (matcher.find()) {
                                    fileName = matcher.group("name")
                                    frame = matcher.group("frame").toInt()
                                }
                                val targetFile = File(assets, fileName.replace('/', File.separatorChar))
                                val targetImage = targetFile
                                    .toImage()
                                    .removeEmptyWidth()
                                    .ifNull("Invalid image: $string")
                                    .toNamed(targetFile.name)
                                (0..<(frame * globalFrame).coerceAtLeast(1)).map {
                                    targetImage
                                }
                            }.sum(),
                            type,
                            configurationSection.getConfigurationSection("setting") ?: emptySetting
                        )
                    }
                }
                imageMap.putSync("image", s) {
                    image
                }
            }
        }
        imageMap.values.forEach { value ->
            val list = value.image
            if (list.isNotEmpty()) {
                list.distinctBy {
                    it.name
                }.forEach {
                    val file = ArrayList(resource.textures).apply {
                        add(it.name)
                    }
                    PackGenerator.addTask(file) {
                        it.image.image.toByteArray()
                    }
                }
            }
        }
    }

    override fun postReload() {
        imageNameComponent.clear()
    }

    override fun end() {
    }
}