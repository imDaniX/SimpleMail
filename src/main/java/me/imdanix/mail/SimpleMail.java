package me.imdanix.mail;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SimpleMail extends JavaPlugin implements Listener {
    private static final Set<String> ARGS = new HashSet<>(Arrays.asList("read", "clear", "send", "help"));

    private Database database;
    private Map<String, List<Mail>> mails;
    private BukkitTask notifier;
    private SimpleDateFormat dateFormat;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();
        database = new Database(this, getConfig().getInt("mails-timeout", 30));
        mails = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void reload() {
        reloadConfig();
        FileConfiguration config = getConfig();
        Message.reload(config.isConfigurationSection("messages") ? config.getConfigurationSection("messages") : config.createSection("messages"));
        dateFormat = new SimpleDateFormat(config.getString("date-format", "dd.MM.yy HH:mm"));
        if (notifier != null) notifier.cancel();
        int ticks = config.getInt("notify-timer", 5) * 1200;
        notifier = ticks > 0 ?
                   Bukkit.getScheduler().runTaskTimer(this, () -> Bukkit.getOnlinePlayers().forEach(this::mailNotify), ticks, ticks) :
                   null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String name = getKey(player);
            List<Mail> mails = database.getMails(name);
            Bukkit.getScheduler().runTask(SimpleMail.this, () -> {
                if (!player.isOnline()) return;
                SimpleMail.this.mails.put(name, mails);
                mailNotify(player);
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        mails.remove(getKey(event.getPlayer()));
    }

    private void mailNotify(Player player) {
        if (!player.hasPermission("simplemail.command")) return;
        List<Mail> mails = this.mails.get(getKey(player));
        if (mails != null && mails.size() > 0) {
            player.sendMessage(Message.NEW_MAILS.get().replace("%mails%", Integer.toString(mails.size())));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            playerMail((Player) sender, args);
        } else {
            sender.sendMessage(ChatColor.RED + "This command can be executed only by player!"); // TODO Allow
        }
        return true;
    }

    private static String getKey(Player player) {
        return player.getName().toLowerCase(Locale.ENGLISH);
    }

    private void playerMail(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage(Message.HELP.get());
            return;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "read": {
                List<Mail> mails = this.mails.get(getKey(player));
                if (mails.size() == 0) {
                    player.sendMessage(Message.NO_MAILS.get());
                    return;
                }
                player.sendMessage(Message.READ_MAILS.get().replace("%mails%", Integer.toString(mails.size())));
                for (Mail mail : mails) {
                    player.sendMessage(Message.MAIL.get()
                            .replace("%sender%", mail.getSender())
                            .replace("%date%", dateFormat.format(mail.getTimestamp()))
                            .replace("%message%", mail.getMessage()));
                }
                return;
            }

            case "clear": {
                String name = getKey(player);
                List<Mail> mails = this.mails.remove(name);
                this.mails.put(name, new ArrayList<>());
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> database.removeMails(name));
                player.sendMessage(Message.CLEAR_MAILS.get().replace("%mails%", Integer.toString(mails.size())));
                return;
            }

            case "send": {
                if (args.length < 3) {
                    player.sendMessage(Message.SEND_MAILS_ARGS.get());
                    return;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (player.hasPermission("simplemail.color")) {
                    message = ChatColor.translateAlternateColorCodes('&', message);
                }
                Mail mail = new Mail(
                        player.getName(),
                        message,
                        System.currentTimeMillis()
                );
                String receiver = args[1].toLowerCase(Locale.ENGLISH);
                if (mails.containsKey(receiver)) {
                    mails.get(receiver).add(mail);
                    mailNotify(getOnline(receiver));
                }
                player.sendMessage(Message.SEND_MAILS.get().replace("%receiver%", args[1]).replace("%message%", message));
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> database.addMail(args[1], mail));
                return;
            }

            case "reload": {
                if (!player.hasPermission("simplemail.reload")) {
                    player.sendMessage(Message.HELP.get());
                    return;
                }
                reload();
                player.sendMessage(ChatColor.GREEN + "Plugin was successfully reloaded!");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], ARGS, new ArrayList<>());
        } else if (args.length == 2) {
            return null;
        } else {
            return Collections.emptyList();
        }
    }

    private static Player getOnline(String name) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }
}
