package com.agrostar.deepak.picsshare.Models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Post {
    private Author author;
    private String full_url;
    private String text;
    private Object timestamp;
    private String full_storage_uri;

    public Post() {

    }

    public Post(Author author, String full_url, String full_storage_uri, String text, Object timestamp) {
        this.author = author;
        this.full_url = full_url;
        this.text = text;
        this.timestamp = timestamp;
        this.full_storage_uri = full_storage_uri;
    }

    public Author getAuthor() {
        return author;
    }

    public String getFull_url() {
        return full_url;
    }

    public String getText() {
        return text;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public String getFull_storage_uri() {
        return full_storage_uri;
    }
}
