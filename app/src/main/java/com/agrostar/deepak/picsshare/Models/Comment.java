package com.agrostar.deepak.picsshare.Models;

public class Comment {
    private Author author;
    private String text;
    private Object timestamp;

    public Comment() {

    }

    public Comment(Author author, String text, Object timestamp) {
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
    }

    public Author getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public Object getTimestamp() {
        return timestamp;
    }
}
