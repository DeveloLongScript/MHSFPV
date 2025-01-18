package me.devlongscript.mhsfpv;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandMessageTask extends BukkitRunnable {
    Player player;
    
    public CommandMessageTask(Player player) {
        this.player = player;
    }
    
    @Override
    public void run() {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        if (!MHSFPV.getInstance().linkedUsers.get(player))
            player.sendMessage(miniMessage.deserialize(MHSFPV.textPrefix + "The code has expired. Run /mhsf again to get another one."));
    }
}
