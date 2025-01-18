package me.devlongscript.mhsfpv;

import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandSuccessTask extends BukkitRunnable {
    private MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor;
    private Player player;
    

    public CommandSuccessTask(MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor, Player player) {
        this.cursor = cursor;
        this.player = player;
    }

    @Override
    public void run() {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        new Thread(() -> {
            while (cursor.hasNext() && !MHSFPV.getInstance().linkedUsers.get(player)) {
                ChangeStreamDocument<Document> change = cursor.next();
                // Check if the operation type is insert
                if ("insert".equals(change.getOperationType().getValue())) {

                    Document insertedDocument = change.getFullDocument();
                    if (insertedDocument != null && player.getName().equals(insertedDocument.getString("player"))) {
                        Bukkit.getScheduler().runTask(MHSFPV.getInstance(), () -> {
                            MHSFPV.getInstance().linkedUsers.put(player, true);
                            player.sendMessage(miniMessage.deserialize(MHSFPV.textPrefix + "You've successfully linked your account!"));
                            player.sendMessage(miniMessage.deserialize(
                                    MHSFPV.textPrefix + "<white><click:open_url:https://github.com/DeveloLongScript/MHSF>Star MHSF on GitHub</click></white> - <#cba402><click:open_url:https://mhsf.app/server/"
                                            + MHSFPV.getInstance().getConfig().getString("serverName") + 
                                            ">Favorite " + MHSFPV.getInstance().getConfig().getString("serverName") + " on MHSF</click>"));
                        });
                        return;
                    }
                }
            }
        }).start();

    }
}
