package me.devlongscript.mhsfpv;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MessageTask extends BukkitRunnable {
    private Player player;
    private String msg;
    private String title;
    private String subtitle;

    public MessageTask(Player player, String msg, String title, String subtitle) {
        this.player = player;
        this.msg = msg;
        this.title = title;
        this.subtitle = subtitle;
    }

    @Override
    public void run() {
        player.sendMessage(msg);
        player.sendTitle(
                title,
                subtitle,
                0,
                40,
                0
        );
    }
}
