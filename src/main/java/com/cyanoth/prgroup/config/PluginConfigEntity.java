package com.cyanoth.prgroup.config;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("PR_GROUP_CONFIG")
public interface PluginConfigEntity extends Entity {
    String COLUMN_MAX_GROUP_SIZE = "MAX_GROUP_SIZE";
    String COLUMN_HIDE_TRUNCATED_GROUPS = "HIDE_TRUNCATED_GROUPS";

    @Accessor(COLUMN_MAX_GROUP_SIZE)
    int getMaxGroupSize();
    void setMaxGroupSize(int maxGroupSize);

    @Accessor(COLUMN_HIDE_TRUNCATED_GROUPS)
    boolean getHideTruncatedGroups();
    void setHideTruncatedGroups(boolean hideTruncatedGroups);
}
