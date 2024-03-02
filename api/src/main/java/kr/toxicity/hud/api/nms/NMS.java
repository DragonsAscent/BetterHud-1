package kr.toxicity.hud.api.nms;

import net.kyori.adventure.text.Component;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface NMS {
    void showBossBar(@NotNull Player player, @NotNull BarColor color, @NotNull Component component);
    void removeBossBar(@NotNull Player player);
    @NotNull NMSVersion getVersion();
}