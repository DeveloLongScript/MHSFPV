package me.devlongscript.mhsfpv;

import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SuccessTask extends BukkitRunnable {
    private MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor;
    private Player player;

    public SuccessTask(MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor, Player player) {
        this.cursor = cursor;
        this.player = player;
    }

    @Override
    public void run() {

        new Thread(() -> {
        while (cursor.hasNext()) {
            ChangeStreamDocument<Document> change = cursor.next();
            // Check if the operation type is insert
            if ("insert".equals(change.getOperationType().getValue())) {

                Document insertedDocument = change.getFullDocument();
                if (insertedDocument != null && player.getName().equals(insertedDocument.getString("player"))) {
                    Bukkit.getScheduler().runTask(MHSFPV.getInstance(), () -> {
                        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', "\n&2&lSuccessfully linked account!\n&2Go to &nlist.mlnehut.com&2 to customize your servers!"));
                    });
                }
            }
        }
        }).start();

    }
}
