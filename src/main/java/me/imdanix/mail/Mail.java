package me.imdanix.mail;

import java.util.Date;

public class Mail {
    private final String sender;
    private final String message;
    private final Date timestamp;

    public Mail(String sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = new Date(timestamp);
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
