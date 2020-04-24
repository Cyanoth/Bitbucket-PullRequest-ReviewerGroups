package com.cyanoth.prgroup;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class PullRequestGroup implements Serializable {
    @SerializedName("group_display_name")
    private final String groupDisplayName;

    @SerializedName("group_members")
    private final ArrayList<PullRequestGroupMember> pullRequestGroupMembers;

    @SerializedName("members_truncated")
    private boolean membersTruncated = false;

    public PullRequestGroup(String groupDisplayName) {
        this.groupDisplayName = groupDisplayName;
        this.pullRequestGroupMembers = new ArrayList<>();
    }

    public ArrayList<PullRequestGroupMember> getPullRequestGroupMembers() {
        return pullRequestGroupMembers;
    }

    public void addGroupMember(PullRequestGroupMember addMember) {
        if (pullRequestGroupMembers.size() >= PluginProperties.MAX_MEMBERS_IN_GROUP) {
            membersTruncated = true;
            return;
        }

        boolean userAlreadyAdded = false;
        for (PullRequestGroupMember member : getPullRequestGroupMembers()) {
            if (member.getUserName().equals(addMember.getUserName()))
                userAlreadyAdded = true;
        }

        if (!userAlreadyAdded)
            pullRequestGroupMembers.add(addMember);
    }

    public boolean isTruncated() {
        return membersTruncated;
    }
}
