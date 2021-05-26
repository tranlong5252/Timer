package com.leontg77.timer.handling.handlers;

import com.leontg77.timer.handling.Actionbar;
import com.leontg77.timer.util.Util;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ActionbarHandler implements Actionbar {

    @Override
    public void sendActionbar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Util.colour(message)));

    }
}
