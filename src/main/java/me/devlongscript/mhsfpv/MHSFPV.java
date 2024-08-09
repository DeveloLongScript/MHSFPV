package me.devlongscript.mhsfpv;

import com.mongodb.client.*;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.mongodb.client.model.Filters.eq;

public final class MHSFPV extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        // Plugin startup logic
        config.addDefault("mongodb", "");
        config.options().copyDefaults(true);
        saveConfig();
        MHSFPV.instance = this;

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        mongoClient.close();
    }
    MongoClient mongoClient ;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        try {
            MongoDatabase database = mongoClient.getDatabase("mhsf");
            MongoCollection collection = database.getCollection("auth_codes");
            collection.findOneAndDelete(eq("player", player.getName()));
        } catch (Exception ignored) {}

    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2&lLink Account"));
        if (mongoClient == null) {
            try {
                mongoClient = MongoClients.create(config.getString("mongodb"));
            } catch(Exception e) {
                config = getConfig();
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&4MongoDB isn't set in the configuration. Cannot continue.\n&4Set database in config."));
            }
        }
            try {
                MongoDatabase database = mongoClient.getDatabase("mhsf");
                MongoCollection collection = database.getCollection("auth_codes");

                MongoCollection users = database.getCollection("claimed-users");
                if (users.find(eq("player", player.getName())).first() != null) {
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&', "\n&4You have already linked your account.\nGo into your account settings to unlink it."));
                    return;
                }
                String code = generateValidCode(collection);
                collection.insertOne(new Document().append("code", code).append("player", player.getName()));


                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Connect your account by typing the code &n" + code + "&2 into list.mlnehut.com"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2This code expires in 60 seconds."));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2"));
                player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', "&2Connect your account by typing the code &n" + code),
                        ChatColor.translateAlternateColorCodes('&', "&2This code expires in 60 seconds.")
                );
                for (int i = 1; i <= 59; ++i) {
                    new MessageTask(player,
                            ChatColor.translateAlternateColorCodes('&', "&2\n&2\n&2\n&2\n&2\n&2\n&2\n&2\n&2&lLink Account\n&2Connect your account by typing the code &n" + code + "&2 into list.mlnehut.com\n&2This code expires in " + (60 - i) + " seconds.\n&2"),
                            ChatColor.translateAlternateColorCodes('&', "&2&lType the code &n" + code),
                            ChatColor.translateAlternateColorCodes('&', "&2This code expires in " + (60 - i) + " seconds.")
                    ).runTaskLater(this, 20 * i);
                }
                MongoCollection watchdb = database.getCollection("claimed-users");
                MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = (MongoChangeStreamCursor<ChangeStreamDocument<Document>>) watchdb.watch().iterator();
                new SuccessTask(cursor, player).runTaskAsynchronously(this);
                new KickTask(player).runTaskLater(this, 20 * 60);
            } catch (Exception e) {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&4MongoDB database is invalid or other exception occured.\n&4Set database in config."));
            }
    }

    public String generateValidCode(MongoCollection collection) {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String finalCode = String.format("%06d", number);

        // figure out if code is already being used
        if (collection.find(eq("code", finalCode)).first() != null) {
            return generateValidCode(collection);
        } else {
            return finalCode;
        }
    }
    private static MHSFPV instance;

    public static MHSFPV getInstance() {
        return MHSFPV.instance;
    }
}
