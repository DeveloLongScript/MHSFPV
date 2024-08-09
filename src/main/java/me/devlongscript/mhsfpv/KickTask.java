package me.devlongscript.mhsfpv;

import com.mongodb.client.MongoCollection;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class KickTask extends BukkitRunnable {

    private Player player;

    public KickTask( Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&4&lCode has expired.\n&4Rejoin to get another code."));
    }
}
