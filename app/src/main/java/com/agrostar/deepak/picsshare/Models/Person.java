package com.agrostar.deepak.picsshare.Models;

import java.util.Map;

public class Person {
    private String displayName;
    private String photoUrl;
    private Map<String, Boolean> posts;
    private Map<String, Object> following;

    public Person() {

    }

    public Person(String displayName, String profile_picture) {
        this.displayName = displayName;
        this.photoUrl = photoUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Map<String, Boolean> getPosts() {
        return posts;
    }

    public Map<String, Object> getFollowing() {
        return following;
    }
}
