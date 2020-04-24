package com.cyanoth.prgroup;

public final class PluginProperties {

    // Only return up to x amount of members from a group.
    // If a group has more than this number, the response should include a field saying it has been truncated.
    // TODO: Candidate to make configurable from global admin
    public static final int MAX_MEMBERS_IN_GROUP = 30;

}
