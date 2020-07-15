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

    @SerializedName("user_id")
    private final int userId;

    public PullRequestGroupMember(int userId, String userDisplayName, String userName, String avatarUrl) {
        this.userId = userId;
        this.userDisplayName = userDisplayName;
        this.userName = userName;
        this.avatarUrl = avatarUrl;
    }
}
