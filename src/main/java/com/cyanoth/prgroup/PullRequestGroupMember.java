package com.cyanoth.prgroup;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class PullRequestGroupMember implements Serializable {
    @SerializedName("user_display_name")
    private final String userDisplayName;

    @SerializedName("user_name")
    private final String userName;

    @SerializedName("user_avatar_url")
    private final String avatarUrl;

    public PullRequestGroupMember(String userDisplayName, String userName, String avatarUrl) {
        this.userDisplayName = userDisplayName;
        this.userName = userName;
        this.avatarUrl = avatarUrl;
    }
}
