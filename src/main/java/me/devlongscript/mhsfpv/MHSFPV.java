package me.devlongscript.mhsfpv;

import com.mongodb.client.*;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.sun.org.apache.bcel.internal.generic.NEW;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.text;

public final class MHSFPV extends JavaPlugin implements Listener, CommandExecutor {
    FileConfiguration config = getConfig();
    public static String textPrefix = "<gradient:#BF00FF:#007BFF>ᴍʜꜱꜰ <#007BFF>| ";
    public HashMap<Player, Boolean> linkedUsers = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        config.addDefault("mongodb", "");
        config.addDefault("linkMethod", "command");
        config.addDefault("serverName", "MHSFPV");
        config.options().copyDefaults(true);
        saveConfig();
        MHSFPV.instance = this;

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("mhsf")).setExecutor(this);
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
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (Objects.equals(config.getString("linkMethod"), "command")) {
                linkedUsers.put(player, false);
                player.sendMessage(miniMessage.deserialize(textPrefix + "Grabbing code from MHSF..."));
                if (mongoClient == null) {
                    try {
                        mongoClient = MongoClients.create(config.getString("mongodb"));
                    } catch(Exception e) {
                        config = getConfig();
                        player.sendMessage(miniMessage.deserialize(textPrefix + "<red>Database hasn't been configured correctly or at all. Please go to a server administrator."));
                        return true;
                    }
                }
                MongoDatabase database = mongoClient.getDatabase("mhsf");
                MongoCollection collection = database.getCollection("auth_codes");

                MongoCollection users = database.getCollection("claimed-users");
                if (users.find(eq("player", player.getName())).first() != null) {
                    player.sendMessage(miniMessage.deserialize(textPrefix + "<red>Your account is already linked! Go to your account settings to unlink it."));
                    return true;
                }
                String code = generateValidCode(collection);
                collection.insertOne(new Document().append("code", code).append("player", player.getName()));
                player.sendMessage(miniMessage.deserialize(textPrefix + "Connect your account by typing the code <bold>" + code + "</bold> into mhsf.app"));
                player.sendMessage(miniMessage.deserialize(textPrefix + "This code expires in 60 seconds."));

                MongoCollection<Document> watchdb = database.getCollection("claimed-users");
                MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = (MongoChangeStreamCursor<ChangeStreamDocument<Document>>) watchdb.watch().iterator();
                new CommandSuccessTask(cursor, player).runTaskAsynchronously(this);
                new CommandMessageTask(player).runTaskLater(this, 60 * 20);
            } else {
                player.sendMessage(miniMessage.deserialize("<red>You cannot link via commands on this server."));
            }
        } else {
            sender.sendMessage("You can't link from the console, silly!");
        }
        
        return true;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (config.getString("linkMethod") != "login") {
            return;
        }
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


                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Connect your account by typing the code &n" + code + "&2 into mhsf.app"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2This code expires in 60 seconds."));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2"));
                player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', "&2Connect your account by typing the code &n" + code),
                        ChatColor.translateAlternateColorCodes('&', "&2This code expires in 60 seconds.")
                );
                for (int i = 1; i <= 59; ++i) {
                    new MessageTask(player,
                            ChatColor.translateAlternateColorCodes('&', "&2\n&2\n&2\n&2\n&2\n&2\n&2\n&2\n&2&lLink Account\n&2Connect your account by typing the code &n" + code + "&2 into mhsf.app\n&2This code expires in " + (60 - i) + " seconds.\n&2"),
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
