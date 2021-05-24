package me.imdanix.mail;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

public enum Message {
    MAIL,
    NEW_MAILS,
    NO_MAILS,
    CLEAR_MAILS,
    SEND_MAILS,
    SEND_MAILS_ARGS,
    READ_MAILS,
    HELP;

    private String message;

    Message() {
        this.message = this.name();
    }

    public String get() {
        return message;
    }

    public static void reload(ConfigurationSection cfg) {
        for (Message msg : Message.values()) {
            msg.message = ChatColor.translateAlternateColorCodes('&',
                    cfg.getString(msg.name().toLowerCase(Locale.ENGLISH).replace('_', '-'), msg.name())
            );
        }
    }
}
