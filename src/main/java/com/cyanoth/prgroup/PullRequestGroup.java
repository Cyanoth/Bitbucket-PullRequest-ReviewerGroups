package com.cyanoth.prgroup;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;

public class PullRequestGroup implements Serializable {
    @SerializedName("group_display_name")
    private final String groupDisplayName;

    @SerializedName("group_members")
    private final ArrayList<PullRequestGroupMember> pullRequestGroupMembers;

    @SerializedName("members_truncated")
    private boolean isMembersTruncated = false;

    public PullRequestGroup(String groupDisplayName) {
        this.groupDisplayName = groupDisplayName;
        this.pullRequestGroupMembers = new ArrayList<>();
    }

    public void addGroupMember(PullRequestGroupMember addMember) {
        pullRequestGroupMembers.add(addMember);
    }

    public boolean isMembersTruncated() {
        return isMembersTruncated;
    }

    public void setIsMembersTruncated(boolean isMembersTruncated) {
        this.isMembersTruncated = isMembersTruncated;
    }
}
