package me.imdanix.mail;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Database {
    private static final long DAYS_TO_MS = 24 * 60 * 60 * 1000;

    private Connection connection;

    public Database(Plugin plugin, int clear) {
        plugin.getDataFolder().mkdir();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "mails.db");
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS `mails` (" +
                    "`receiver` VARCHAR(64) NOT NULL," +
                    "`sender` VARCHAR(64) NOT NULL," +
                    "`message` VARCHAR NOT NULL," +
                    "`timestamp` INTEGER NOT NULL" +
            ");");
            clear(clear);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void clear(long days) throws SQLException {
        if (days == -1) return;
        long minTime = System.currentTimeMillis() - days * DAYS_TO_MS;
        PreparedStatement statement = connection.prepareStatement("DELETE FROM `mails` WHERE `timestamp` < ?;");
        statement.setLong(1, minTime);
        statement.executeUpdate();
    }

    public List<Mail> getMails(String receiver) {
        List<Mail> messages = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `mails` WHERE `receiver` = ?;");
            statement.setString(1, receiver.toLowerCase(Locale.ENGLISH));
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                messages.add(new Mail(result.getString("sender"), result.getString("message"), result.getLong("timestamp")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public void addMail(String receiver, Mail message) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `mails` (receiver,sender,message,timestamp) VALUES (?,?,?,?);");
            statement.setString(1, receiver.toLowerCase(Locale.ENGLISH));
            statement.setString(2, message.getSender());
            statement.setString(3, message.getMessage());
            statement.setLong(4, message.getTimestamp().getTime());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMail(String receiver, String sender, long timestamp) {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `mails` WHERE `receiver` = ? AND `sender` = ? AND `timestamp` = ?;");
            statement.setString(1, receiver.toLowerCase(Locale.ENGLISH));
            statement.setString(2, sender);
            statement.setLong(3, timestamp);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
